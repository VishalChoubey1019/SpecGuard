-- V1: initial schema
-- Single Postgres schema, no per-service isolation (see LOE: monolith architecture).

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- tenant_id is stubbed but unused in v1: no multi-tenant enforcement yet.
    -- Every row gets a default tenant so the column is present and populated
    -- from day one, making a future multi-tenant migration a backfill-free refactor.
    tenant_id       UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
