-- V2: pull_requests table, populated from GitHub webhook events.

CREATE TABLE pull_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',

    repo_full_name  VARCHAR(255) NOT NULL,   -- e.g. "org/repo"
    pr_number       INTEGER NOT NULL,        -- per-repo PR number, e.g. 42
    github_pr_id    BIGINT NOT NULL,         -- GitHub's global PR id, stable across renumbering
    title           VARCHAR(1024) NOT NULL,
    author_login    VARCHAR(255) NOT NULL,
    base_sha        VARCHAR(64) NOT NULL,
    head_sha        VARCHAR(64) NOT NULL,
    status          VARCHAR(32) NOT NULL,    -- OPEN | CLOSED | MERGED

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_pull_requests_repo_pr UNIQUE (repo_full_name, pr_number)
);

CREATE INDEX idx_pull_requests_tenant_id ON pull_requests (tenant_id);
CREATE INDEX idx_pull_requests_github_pr_id ON pull_requests (github_pr_id);