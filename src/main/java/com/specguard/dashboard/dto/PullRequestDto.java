package com.specguard.dashboard.dto;

import com.specguard.repo.PrStatus;

import java.util.UUID;

/**
 * Dashboard-facing view of a {@link com.specguard.repo.PullRequest}.
 *
 * @param riskScore total across this PR's violations, per
 *                  {@link com.specguard.governance.RiskScoreCalculator#scoreForPullRequest} —
 *                  not stored, computed at read time.
 */
public record PullRequestDto(UUID id, String repoFullName, int prNumber, String title, PrStatus status,
                              int riskScore) {}