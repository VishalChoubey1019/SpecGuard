-- V3: violations table. One row per policy violation detected on a pull request.
-- Populated by governance analyzers (OpenAPI diff first; SQL/Dockerfile/PR-metadata in Phase 3).

CREATE TABLE violations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    pull_request_id   UUID NOT NULL REFERENCES pull_requests (id) ON DELETE CASCADE,
    category          VARCHAR(32) NOT NULL,   -- API_CONTRACT | SQL_MIGRATION | DOCKERFILE | PR_METADATA
    severity          VARCHAR(16) NOT NULL,   -- LOW | MEDIUM | HIGH | CRITICAL
    rule_id           VARCHAR(255) NOT NULL,  -- analyzer-specific rule identifier, e.g. oasdiff's check id
    message           TEXT NOT NULL,
    file_path         VARCHAR(1024),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_violations_pull_request_id ON violations (pull_request_id);
CREATE INDEX idx_violations_tenant_id ON violations (tenant_id);