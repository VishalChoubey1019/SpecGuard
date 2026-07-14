package com.specguard.repo;

import com.specguard.governance.Violation;
import com.specguard.governance.ViolationRepo;
import com.specguard.governance.openapi.OpenApiDiffAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Handles incoming {@code pull_request} webhook events: upserts the {@link PullRequest}
 * row, fetches changed files, and runs the governance analyzers against them.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PullRequestService {

    /** Webhook actions that mean the PR has code worth analyzing. Everything else is ignored. */
    private static final Set<String> ANALYZABLE_ACTIONS = Set.of("opened", "synchronize", "reopened");

    private final PullRequestRepo pullRequestRepository;
    private final GithubClient gitHubClient;
    private final OpenApiDiffAnalyzer openApiDiffAnalyzer;
    private final ViolationRepo violationRepository;

    /**
     * Processes one {@code pull_request} webhook event: upserts the PR record, and if the
     * action indicates new/changed code, fetches the changed files and runs analyzers
     * against them, persisting any violations found.
     *
     * @param event the parsed GitHub webhook payload
     * @return violations found for this event, or an empty list if the action wasn't one
     *         that warrants analysis (e.g. a label change)
     */
    public List<Violation> handleEvent(GithubPullRequestEvent event) {
        PullRequest pr = upsert(event);

        if (!ANALYZABLE_ACTIONS.contains(event.action())) {
            log.debug("Ignoring non-analyzable action '{}' for {}#{}",
                    event.action(), pr.getRepoFullName(), pr.getPrNumber());
            return List.of();
        }

        List<ChangedFile> changedFiles = gitHubClient.fetchChangedFiles(pr.getRepoFullName(), pr.getPrNumber());

        // TODO(phase-3): SQL migration (Squawk), Dockerfile (Hadolint), and PR-metadata
        // analyzers join here once they exist — same pattern as OpenApiDiffAnalyzer.
        List<Violation> violations = openApiDiffAnalyzer.analyze(pr, changedFiles);

        if (!violations.isEmpty()) {
            violationRepository.saveAll(violations);
            log.info("Recorded {} violation(s) for {}#{}", violations.size(), pr.getRepoFullName(), pr.getPrNumber());
        }

        return violations;
    }

    private PullRequest upsert(GithubPullRequestEvent event) {
        var payload = event.pullRequest();
        String repoFullName = event.repository().fullName();
        PrStatus status = resolveStatus(event.action(), payload.merged());

        PullRequest pr = pullRequestRepository
                .findByRepoFullNameAndPrNumber(repoFullName, payload.number())
                .map(existing -> {
                    existing.applyUpdate(payload.title(), payload.base().sha(), payload.head().sha(), status);
                    log.info("Updated tracked PR {}#{} (action={})", repoFullName, payload.number(), event.action());
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("Tracking new PR {}#{} (action={})", repoFullName, payload.number(), event.action());
                    return new PullRequest(
                            repoFullName, payload.number(), payload.id(), payload.title(),
                            payload.user().login(), payload.base().sha(), payload.head().sha(), status);
                });

        return pullRequestRepository.save(Objects.requireNonNull(pr));
    }

    private PrStatus resolveStatus(String action, boolean merged) {
        if ("closed".equals(action)) {
            return merged ? PrStatus.MERGED : PrStatus.CLOSED;
        }
        return PrStatus.OPEN;
    }
}