package com.specguard.governance;

/**
 * Which governance domain a {@link Violation} came from. Mirrors the four analyzer
 * types in the LOE: API contracts (OpenAPI), database migrations (SQL), containers
 * (Dockerfiles), and process (PR metadata).
 */
public enum ViolationCategory {
    API_CONTRACT,
    SQL_MIGRATION,
    DOCKERFILE,
    PR_METADATA
}