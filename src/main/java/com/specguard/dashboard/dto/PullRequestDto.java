package com.specguard.dashboard.dto;

import com.specguard.repo.PrStatus;

import java.util.UUID;

/** Dashboard-facing view of a {@link com.specguard.repo.PullRequest}. */
public record PullRequestDto(UUID id, String repoFullName, int prNumber, String title, PrStatus status) {}