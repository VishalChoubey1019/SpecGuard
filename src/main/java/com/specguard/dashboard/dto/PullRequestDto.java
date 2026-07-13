package com.specguard.dashboard.dto;

import java.util.UUID;

/** Dashboard-facing view of a {@link com.specguard.repo.PullRequest}. */
public record PullRequestDto(UUID id, String repoFullName, int prNumber) {}
