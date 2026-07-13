package com.specguard.governance;

import com.specguard.auth.User;
import com.specguard.repo.PullRequest;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * A single policy violation found on a pull request by one of the governance analyzers
 * (OpenAPI diff, SQL migration check, Dockerfile check, PR metadata linter).
 */
@Entity
@Table(name = "violations")
@Getter
public class Violation {

    @Id
    @GeneratedValue
    private UUID id;

    // Stubbed for future multi-tenant support, same pattern as User/PullRequest.
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = User.DEFAULT_TENANT_ID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationCategory category;
}