package com.specguard.governance;

/**
 * How serious a {@link Violation} is. Set by SpecGuard's own analyzer logic (e.g. an
 * OpenAPI analyzer mapping the underlying tool's severity scale to this enum) — not
 * copied from GitHub or any external source.
 */
public enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}