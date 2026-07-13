package com.specguard.repo;

import com.specguard.config.GithubProperties;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Thin wrapper over the GitHub REST API (via org.kohsuke:github-api) for what this module
 * needs: a PR's changed files, and reading a file's content at a specific commit.
 * Contains a constructor for github auth and two methods: 
 * - fetching all the files from the PR, 
 * - fetching all the content of a file 
 */
@Component
@Slf4j
public class GithubClient {

    private final GitHub github;

    /**
     * Builds the underlying {@link GitHub} connection once at startup rather than
     * per-request — the connection is stateless/reusable, and re-authenticating on every
     * webhook delivery would add avoidable latency and load to every single call.
     *
     * @throws GitHubApiException if the connection can't be established (bad token, network)
     */
    public GithubClient(GithubProperties properties) {
        try {
            this.github = new GitHubBuilder().withOAuthToken(properties.getAppToken()).build();
        } catch (IOException e) {
            throw new GithubApiException("Failed to initialize GitHub API client", e);
        }
    }

    /**
     * @param repoFullName GitHub {@code org/repo} slug
     * @param prNumber     per-repo PR number
     * @return every file changed in the PR, with diff patches where GitHub provides one
     * @throws GitHubApiException if the GitHub API call fails (auth, network, rate limit, etc.)
     */
    public List<ChangedFile> fetchChangedFiles(String repoFullName, int prNumber) {
        try {
            GHRepository repo = github.getRepository(repoFullName);
            GHPullRequest pr = repo.getPullRequest(prNumber);
            
            List<ChangedFile> files = pr.listFiles().toList().stream()
                    .map(this::toChangedFile)
                    .toList();

            log.info("Fetched {} changed file(s) for {}#{}", files.size(), repoFullName, prNumber);
            return files;
        } catch (IOException e) {
            throw new GithubApiException(
                    "Failed to fetch changed files for " + repoFullName + "#" + prNumber, e);
        }
    }

    /**
     * Reads a file's content as it existed at a specific commit. Used to fetch both sides
     * (base and head) of a changed file for diffing.
     *
     * @param repoFullName GitHub {@code org/repo} slug
     * @param path         repo-relative file path
     * @param ref          commit SHA, branch, or tag to read the file at
     * @return the file's decoded text content, or empty if the file doesn't exist at that ref
     *         (e.g. it was added in this PR, so it has no base version)
     * @throws GitHubApiException if the GitHub API call fails for any reason other than the
     *                            file simply not existing at that ref
     */
    public Optional<String> fetchFileContent(String repoFullName, String path, String ref) {
        try {
            GHContent content = github.getRepository(repoFullName).getFileContent(path, ref);
            InputStream inputStream = content.read();
            String fileContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return Optional.of(fileContent);
        } catch (GHFileNotFoundException e) {
            log.warn("[GHFileNotFoundException] File not found");
            return Optional.empty();
        } catch (IOException e) {
            throw new GithubApiException(
                    "Failed to fetch " + path + "@" + ref + " in " + repoFullName, e);
        }
    }

    private ChangedFile toChangedFile(GHPullRequestFileDetail file) {
        return new ChangedFile(
                file.getFilename(),
                file.getStatus(),
                file.getAdditions(),
                file.getDeletions(),
                file.getPatch());
    }
}