-- V4: repositories + per-user access.
-- Fills the "which repos can this user see" gap: V1..V3 had users, pull_requests
-- (free-text repo_full_name), and violations, but no repository entity and no
-- user<->repo mapping. pull_requests.repo_full_name stays free-text for now and
-- joins to repositories.full_name by value (no FK added yet — deliberate, avoids a
-- backfill on existing PR rows).

CREATE TABLE repositories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',

    -- GitHub's global numeric repo id: stable identity, survives rename/transfer.
    -- Same pattern as pull_requests.github_pr_id vs the mutable repo_full_name/pr_number.
    github_repo_id  BIGINT NOT NULL,
    -- Mutable display/join key. Changes on repo rename; update this row in place,
    -- github_repo_id stays fixed. Joins to pull_requests.repo_full_name by value.
    full_name       VARCHAR(255) NOT NULL,   -- "org/repo"
    provider        VARCHAR(32) NOT NULL DEFAULT 'GITHUB',
    default_branch  VARCHAR(255),
    private         BOOLEAN NOT NULL DEFAULT false,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_repositories_github_repo_id UNIQUE (github_repo_id),
    CONSTRAINT uq_repositories_full_name UNIQUE (full_name)
);

CREATE INDEX idx_repositories_tenant_id ON repositories (tenant_id);

-- Many-to-many: one repo visible to many users, one user sees many repos.
-- role scopes what the user may do (OWNER can manage access, VIEWER read-only).
CREATE TABLE repository_access (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',

    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    repository_id   UUID NOT NULL REFERENCES repositories (id) ON DELETE CASCADE,
    role            VARCHAR(16) NOT NULL DEFAULT 'VIEWER',   -- OWNER | VIEWER

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_repository_access_user_repo UNIQUE (user_id, repository_id)
);

CREATE INDEX idx_repository_access_tenant_id ON repository_access (tenant_id);
CREATE INDEX idx_repository_access_user_id ON repository_access (user_id);
CREATE INDEX idx_repository_access_repository_id ON repository_access (repository_id);
