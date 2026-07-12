package com.specguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpecGuard MVP — single monolith entrypoint.
 *
 * Domain modules live as packages, not services:
 *   repo/        - GitHub webhook receipt, changed-file fetch
 *   governance/  - policy engine, analyzers (OpenAPI/SQL/Dockerfile/PR-metadata), violations, risk scoring
 *   workflow/    - severity->action automation (Jira, Slack, PR comments), retry/dedupe
 *   dashboard/   - REST endpoints + served frontend
 *   copilot/     - LLM explanation of violations
 *   auth/        - single-tenant email/password + JWT auth
 *
 * No event bus. No microservices. One Postgres schema. See LOE doc for scope boundaries.
 */
@SpringBootApplication
public class SpecGuardApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpecGuardApplication.class, args);
    }
}
