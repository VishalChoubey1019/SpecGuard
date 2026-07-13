package com.specguard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Paths to the external CLI tools governance analyzers wrap, rather than hand-rolling
 * (oasdiff for OpenAPI diffs now; Squawk and Hadolint join here in Phase 3).
 * Defaults assume the binary is on {@code PATH} — override via env if it's installed elsewhere.
 */
@Component
@ConfigurationProperties(prefix = "specguard.governance")
@Data
public class GovernanceProperties {
    /** OpenApiDiffToolPath used in OpenApiDiffAnalyzer */
    private String oasdiffPath = "oasdiff";
}