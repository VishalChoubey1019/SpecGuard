package com.specguard.repo;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * A GitHub pull request tracked by SpecGuard, upserted from incoming webhook events.
 * One row per (repo, PR number); re-opened/synchronized PRs update the existing row
 * rather than creating a new one.
 *
 * Every field below comes from GitHub's {@code pull_request} webhook payload, EXCEPT
 * id/tenantId/createdAt/updatedAt (SpecGuard-generated, same pattern as User) and
 * status (SpecGuard derives it, doesn't copy it — see comment below).
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

    // Source: webhook payload's repository.full_name (e.g. "octocat/Hello-World")
    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    // Source: webhook payload's pull_request.number — resets if a repo is ever recreated,
    // which is why githubPrId below exists as a second, permanently-stable identifier.
    @Column(name = "pr_number", nullable = false)
    private int prNumber;

    // Source: webhook payload's pull_request.id — GitHub's global PR id, stable even
    // across repo transfers/renumbering, unlike prNumber. Required NOT NULL by the V2
    // migration (github_pr_id column) — this was missing before and would have failed
    // every insert with a not-null constraint violation once webhook handling existed.
    @Column(name = "github_pr_id", nullable = false)
    private long githubPrId;

    // Source: webhook payload's pull_request.title
    @Column(nullable = false)
    private String title;

    // Source: webhook payload's pull_request.user.login. Also required NOT NULL by the
    // V2 migration (author_login column) — same missing-field issue as githubPrId above.
    @Column(name = "author_login", nullable = false)
    private String authorLogin;

    // Source: webhook payload's pull_request.base.sha — the commit SHA of the target
    // branch BEFORE this PR's changes. Needed to fetch the "old" version of a changed
    // file (e.g. old openapi.yaml) so it can be diffed against the new version.
    @Column(name = "base_sha", nullable = false)
    private String baseSha;

    // Source: webhook payload's pull_request.head.sha — the commit SHA of the PR's
    // latest commit. Needed to fetch the "new" version of a changed file.
    @Column(name = "head_sha", nullable = false)
    private String headSha;

    // NOT copied directly from GitHub — SpecGuard derives this itself from the webhook's
    // action field (e.g. "opened"/"synchronize" -> OPEN, "closed" + merged=true -> MERGED).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrStatus status;

    // SpecGuard-generated, same pattern as User.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PullRequest() {
        // JPA
    }

    public PullRequest(String repoFullName, int prNumber, long githubPrId, String title,
                        String authorLogin, String baseSha, String headSha, PrStatus status) {
        this.repoFullName = repoFullName;
        this.prNumber = prNumber;
        this.githubPrId = githubPrId;
        this.title = title;
        this.authorLogin = authorLogin;
        this.baseSha = baseSha;
        this.headSha = headSha;
        this.status = status;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}