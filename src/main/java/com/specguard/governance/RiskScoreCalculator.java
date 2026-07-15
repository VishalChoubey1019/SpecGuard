package com.specguard.governance;

import com.specguard.repo.PrStatus;
import com.specguard.repo.PullRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Computes a "risk score" for a pull request or repo as a weighted sum of its open
 * violations by severity.
 */
@Component
@RequiredArgsConstructor
public class RiskScoreCalculator {

    /**
     * Points per violation at each severity. Arbitrary but monotonic weighting, not derived
     * from any external source — revisit if a stakeholder wants a specific formula instead.
     */
    private static final Map<Severity, Integer> SEVERITY_WEIGHTS = new EnumMap<>(Map.of(
            Severity.LOW, 1,
            Severity.MEDIUM, 3,
            Severity.HIGH, 7,
            Severity.CRITICAL, 15));

    private final ViolationRepo violationRepo;

    /**
     * @param pullRequest a tracked pull request
     * @return sum of severity weights across every violation recorded for that PR
     */
    public int scoreForPullRequest(PullRequest pullRequest) {
        return score(violationRepo.findByPullRequest(pullRequest));
    }

    /**
     * @param repoFullName GitHub {@code org/repo} slug
     * @return sum of severity weights across every violation ever recorded for the repo,
     *         including violations on PRs that are now merged or closed
     */
    public int scoreForRepo(String repoFullName) {
        return score(violationRepo.findByPullRequest_RepoFullName(repoFullName));
    }

    /**
     * @param repoFullName GitHub {@code org/repo} slug
     * @return sum of severity weights across violations on only the repo's currently-open
     *         pull requests — the actionable subset, unlike {@link #scoreForRepo}
     */
    public int scoreForRepoOpenOnly(String repoFullName) {
        return score(violationRepo.findByPullRequest_RepoFullNameAndPullRequest_Status(repoFullName, PrStatus.OPEN));
    }

    private int score(List<Violation> violations) {
        return violations.stream().mapToInt(v -> SEVERITY_WEIGHTS.get(v.getSeverity())).sum();
    }
}