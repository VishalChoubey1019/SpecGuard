package com.specguard.dashboard.dto;

import com.specguard.repo.PrStatus;

import java.util.UUID;

public record PullRequestDto(UUID id, String repoFullName, int prNumber, String title, PrStatus status,
                              int riskScore) {}