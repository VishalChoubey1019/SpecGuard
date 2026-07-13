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
 *
 * Unlike PullRequest/ChangedFile, these fields are NOT copied from GitHub — they're
 * SpecGuard's own output, built while analyzing a PR:
 *   category  - which analyzer ran (SpecGuard sets this, e.g. API_CONTRACT)
 *   severity  - SpecGuard's own mapping (e.g. oasdiff's numeric "level" -> this enum)
 *   ruleId    - copied from the underlying tool's output (e.g. oasdiff's "id" field,
 *               such as "response-property-removed") — external, but from the analyzer
 *               tool, not from GitHub
 *   message   - copied from the underlying tool's human-readable description
 *               (e.g. oasdiff's "text" field)
 *   filePath  - the file SpecGuard was analyzing when it found this violation — known
 *               because SpecGuard is the one iterating the changed files, not fetched
 *               from anywhere external
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(name = "rule_id", nullable = false)
    private String ruleId;

    @Column(nullable = false)
    private String message;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Violation() {
        // JPA
    }

    public Violation(PullRequest pullRequest, ViolationCategory category, Severity severity,
                      String ruleId, String message, String filePath) {
        this.pullRequest = pullRequest;
        this.category = category;
        this.severity = severity;
        this.ruleId = ruleId;
        this.message = message;
        this.filePath = filePath;
    }
}