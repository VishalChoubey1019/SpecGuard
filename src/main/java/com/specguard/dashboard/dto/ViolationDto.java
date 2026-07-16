package com.specguard.dashboard.dto;

import com.specguard.governance.Severity;
import com.specguard.governance.ViolationCategory;

import java.util.UUID;

public record ViolationDto(UUID id, UUID pullRequestId, ViolationCategory category, Severity severity,
                            String ruleId, String message, String filePath, int riskScore) {}