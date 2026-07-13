package com.specguard.dashboard.dto;

import com.specguard.governance.Severity;
import com.specguard.governance.ViolationCategory;

import java.util.UUID;

/** Dashboard-facing view of a {@link com.specguard.governance.Violation}. */
public record ViolationDto(UUID id, UUID pullRequestId, ViolationCategory category, Severity severity) {}
