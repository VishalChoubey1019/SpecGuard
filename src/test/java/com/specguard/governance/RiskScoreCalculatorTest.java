package com.specguard.governance;

import com.specguard.repo.PrStatus;
import com.specguard.repo.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RiskScoreCalculator}'s weighted-sum formula (LOW=1, MEDIUM=3,
 * HIGH=7, CRITICAL=15) and its total-vs-open-only distinction. Pure Mockito mocks of
 * {@link ViolationRepo} — no Spring context or database needed, same pattern as the
 * OpenApiDiffAnalyzer tests. Does NOT verify that {@link ViolationRepo}'s derived query
 * method names actually resolve against the entity graph; that only happens at
 * application-context startup (see {@link ViolationRepo}'s class javadoc) and needs a
 * real boot or a {@code @DataJpaTest} slice, neither of which this test performs.
 */
@ExtendWith(MockitoExtension.class)
class RiskScoreCalculatorTest {

    private static final String REPO = "octocat/Hello-World";

    @Mock
    private ViolationRepo violationRepo;

    private RiskScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskScoreCalculator(violationRepo);
    }

    @Test
    void scoreForPullRequest_returnsZero_whenNoViolations() {
        PullRequest pr = pullRequest(PrStatus.OPEN);
        when(violationRepo.findByPullRequest(pr)).thenReturn(List.of());

        assertThat(calculator.scoreForPullRequest(pr)).isEqualTo(0);
    }

    @Test
    void scoreForPullRequest_sumsWeightsAcrossMixedSeverities() {
        PullRequest pr = pullRequest(PrStatus.OPEN);
        when(violationRepo.findByPullRequest(pr)).thenReturn(List.of(
                violation(pr, Severity.LOW),      // 1
                violation(pr, Severity.MEDIUM),    // 3
                violation(pr, Severity.HIGH),      // 7
                violation(pr, Severity.CRITICAL))); // 15

        assertThat(calculator.scoreForPullRequest(pr)).isEqualTo(1 + 3 + 7 + 15);
    }

    @Test
    void scoreForPullRequest_weighsMultipleViolationsOfSameSeverity() {
        PullRequest pr = pullRequest(PrStatus.OPEN);
        when(violationRepo.findByPullRequest(pr)).thenReturn(List.of(
                violation(pr, Severity.CRITICAL),
                violation(pr, Severity.CRITICAL)));

        assertThat(calculator.scoreForPullRequest(pr)).isEqualTo(15 + 15);
    }

    @Test
    void scoreForRepo_includesViolationsAcrossAllPrStatuses() {
        PullRequest openPr = pullRequest(PrStatus.OPEN);
        PullRequest mergedPr = pullRequest(PrStatus.MERGED);
        when(violationRepo.findByPullRequest_RepoFullName(REPO)).thenReturn(List.of(
                violation(openPr, Severity.HIGH),     // 7
                violation(mergedPr, Severity.MEDIUM))); // 3, still counted here

        assertThat(calculator.scoreForRepo(REPO)).isEqualTo(7 + 3);
    }

    @Test
    void scoreForRepoOpenOnly_onlyQueriesOpenStatus() {
        PullRequest openPr = pullRequest(PrStatus.OPEN);
        when(violationRepo.findByPullRequest_RepoFullNameAndPullRequest_Status(REPO, PrStatus.OPEN))
                .thenReturn(List.of(violation(openPr, Severity.CRITICAL))); // 15

        assertThat(calculator.scoreForRepoOpenOnly(REPO)).isEqualTo(15);
    }

    @Test
    void scoreForRepoOpenOnly_returnsZero_whenNoOpenPrsHaveViolations() {
        when(violationRepo.findByPullRequest_RepoFullNameAndPullRequest_Status(REPO, PrStatus.OPEN))
                .thenReturn(List.of());

        assertThat(calculator.scoreForRepoOpenOnly(REPO)).isEqualTo(0);
    }

    private static PullRequest pullRequest(PrStatus status) {
        return new PullRequest(REPO, 42, 123456789L, "Add feature",
                "octocat", "base-sha", "head-sha", status);
    }

    private static Violation violation(PullRequest pr, Severity severity) {
        return new Violation(pr, ViolationCategory.API_CONTRACT, severity,
                "some-rule-id", "some message", "openapi.yaml");
    }
}