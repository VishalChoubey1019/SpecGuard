package com.specguard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * specguard.jwt.* config, bound from env vars (see .env.example).
 * Extend this only when a phase actually starts reading a new key (YAGNI) —
 * e.g. add a `github` block when Phase 2's webhook receiver needs GITHUB_WEBHOOK_SECRET.
 */
@Component
@ConfigurationProperties(prefix = "specguard.jwt")
@Data
public class SpecGuardProperties {
    private String secret;
    private long expirationMinutes;
}
