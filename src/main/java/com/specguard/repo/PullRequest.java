package com.specguard.repo;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * A GitHub pull request tracked by SpecGuard, upserted from incoming webhook events.
 * One row per (repo, PR number); re-opened/synchronized PRs update the existing row
 * rather than creating a new one.
 */
@Entity
@Table(name = "pull_requests")
@Getter

public class PullRequest {

    @Id
    @GeneratedValue
    private UUID id;

    // Stubbed for future multi-tenant support, same as User — not enforced in v1.
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = com.specguard.auth.User.DEFAULT_TENANT_ID;

    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    @Column(name = "pr_number", nullable = false)
    private int prNumber;
    // Todo: add or remove things if required
}