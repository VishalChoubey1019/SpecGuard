package com.specguard.governance.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specguard.config.GovernanceProperties;
import com.specguard.governance.Severity;
import com.specguard.governance.Violation;
import com.specguard.governance.ViolationCategory;
import com.specguard.repo.ChangedFile;
import com.specguard.repo.GithubClient;
import com.specguard.repo.PullRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Detects breaking API changes in OpenAPI spec files touched by a pull request, by shelling
 * out to {@code oasdiff breaking} rather than reimplementing spec comparison. oasdiff checks
 * 300+ distinct categories of breaking change across the full OpenAPI spec (see
 * https://www.oasdiff.com) — reimplementing even a fraction of that ourselves would dwarf
 * this file. It has no JVM library, only a Go CLI/binary, so wrapping it as a subprocess is
 * the only "wrap, don't hand-roll" option here (see {@link ExternalProcessRunner}).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OpenApiDiffAnalyzer {

    /**
     * Matches likely OpenAPI/Swagger spec files by filename convention. Deliberately simple
     * for now — TODO(phase-2-next): make this configurable via the policy engine instead of
     * a hardcoded pattern, once that exists.
     */
    private static final Pattern OPENAPI_FILENAME = Pattern.compile(
            "(?i).*(openapi|swagger).*\\.(ya?ml|json)$");

    private final GithubClient gitHubClient;
    private final GovernanceProperties governanceProperties;
    private final ExternalProcessRunner processRunner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analyzes every changed file in {@code changedFiles} that looks like an OpenAPI spec.
     * Only files with status {@code modified} are diffed — added files have no prior contract
     * to break, and removed/renamed files aren't handled yet (see TODO below).
     *
     * @param pullRequest the PR the changed files belong to (source of base/head SHAs)
     * @param changedFiles files changed in this PR, as fetched from GitHub
     * @return one {@link Violation} per breaking change oasdiff reports, across all matching files
     */
    public List<Violation> analyze(PullRequest pullRequest, List<ChangedFile> changedFiles) {
        return changedFiles.stream()
                .filter(f -> OPENAPI_FILENAME.matcher(f.filename()).matches())
                .filter(f -> "modified".equals(f.status()))
                // TODO(phase-later): handle "added" (flag nothing, it's a new contract) and
                // "removed" (arguably always breaking — whole API surface disappeared) explicitly,
                // and "renamed" (diff old path's content against new path's content).
                .flatMap(f -> {
                    // Catches per file, not around the whole stream: one file's GitHub/oasdiff
                    // failure shouldn't abort analysis of every other changed file in the PR.
                    try {
                        return analyzeFile(pullRequest, f.filename()).stream();
                    } catch (RuntimeException e) {
                        log.error("Skipping {} after analyzer failure", f.filename(), e);
                        return Stream.empty();
                    }
                })
                .toList();
    }

    private List<Violation> analyzeFile(PullRequest pullRequest, String filePath) {
        Optional<String> baseContent = gitHubClient.fetchFileContent(
                pullRequest.getRepoFullName(), filePath, pullRequest.getBaseSha());
        Optional<String> headContent = gitHubClient.fetchFileContent(
                pullRequest.getRepoFullName(), filePath, pullRequest.getHeadSha());

        if (baseContent.isEmpty() || headContent.isEmpty()) {
            log.debug("Skipping {}: missing base or head content (unexpected for a 'modified' file)", filePath);
            return List.of();
        }

        Path baseFile = null;
        Path headFile = null;
        try {
            baseFile = writeTemp("oasdiff-base-", filePath, baseContent.get());
            headFile = writeTemp("oasdiff-head-", filePath, headContent.get());

            String stdout = processRunner.run(List.of(
                    governanceProperties.getOasdiffPath(), "breaking",
                    baseFile.toString(), headFile.toString(),
                    "-f", "json"));

            return parseBreakingChanges(stdout, pullRequest, filePath);
        } catch (IOException e) {
            log.error("Failed to write temp files for oasdiff on {}", filePath, e);
            return List.of();
        } finally {
            deleteQuietly(baseFile);
            deleteQuietly(headFile);
        }
    }

    /**
     * Parses oasdiff's {@code breaking -f json} output. Schema confirmed via {@code oasdiff
     * schema}: an array of Change objects with {@code id} (string), {@code text} (string), and
     * {@code level} (integer: ERR=3, WARN=2, INFO=1) among other fields we don't use yet
     * (comment, operation, path, section, source locations). Parsed via {@link JsonNode} rather
     * than a strict POJO since we only need three of its fields — revisit if more get used.
     */
    private List<Violation> parseBreakingChanges(String stdout, PullRequest pullRequest, String filePath) {
        if (stdout.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(stdout);
            if (!root.isArray()) {
                log.warn("Unexpected oasdiff output shape for {} (expected a JSON array): {}", filePath, stdout);
                return List.of();
            }

            return StreamSupport.stream(root.spliterator(), false)
                    .map(node -> toViolation(node, pullRequest, filePath))
                    .toList();
        } catch (IOException e) {
            log.error("Failed to parse oasdiff output for {}: {}", filePath, stdout, e);
            return List.of();
        }
    }

    private Violation toViolation(JsonNode node, PullRequest pullRequest, String filePath) {
        String ruleId = node.path("id").asText("unknown-oasdiff-check");
        String text = node.path("text").asText(node.toString());
        Severity severity = mapSeverity(node.path("level").asInt(-1));

        return new Violation(pullRequest, ViolationCategory.API_CONTRACT, severity, ruleId, text, filePath);
    }

    /**
     * Maps oasdiff's numeric level (confirmed integer via {@code oasdiff schema}: ERR=3,
     * WARN=2, INFO=1) to our Severity. Unverified whether {@code breaking} can ever emit
     * INFO — every real example we've seen only shows ERR/WARN — so INFO is mapped rather
     * than assumed unreachable, and unrecognized values default to MEDIUM with a warning
     * instead of failing the whole violation.
     */
    private Severity mapSeverity(int oasdiffLevel) {
        return switch (oasdiffLevel) {
            case 3 -> Severity.CRITICAL; // ERR
            case 2 -> Severity.HIGH;     // WARN
            case 1 -> Severity.MEDIUM;   // INFO
            default -> {
                log.warn("Unrecognized oasdiff level {}, defaulting to MEDIUM", oasdiffLevel);
                yield Severity.MEDIUM;
            }
        };
    }

    /**
     * @throws java.lang.StringIndexOutOfBoundsException if {@code originalFilename} has no
     *         extension — safe today only because {@link #OPENAPI_FILENAME} guarantees every
     *         file reaching this method ends in .yaml/.yml/.json; revisit if that regex loosens.
     */
    private Path writeTemp(String prefix, String originalFilename, String content) throws IOException {
        String suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        Path path = Files.createTempFile(prefix, suffix);
        Files.writeString(path, content);
        return path;
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}", path, e);
        }
    }
}