package com.specguard.dashboard.dto;

import com.specguard.governance.Severity;
import com.specguard.governance.ViolationCategory;

import java.util.UUID;

/**
 * Dashboard-facing view of a {@link com.specguard.governance.Violation}.
 *
 * @param riskScore the point weight this single violation contributes, per
 *                  {@link com.specguard.governance.RiskScoreCalculator#weightFor} — not a
 *                  stored field, derived from severity at read time.
 */
public record ViolationDto(UUID id, UUID pullRequestId, ViolationCategory category, Severity severity,
                            String ruleId, String message, String filePath, int riskScore) {}