package com.specguard.auth;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    // Stubbed for future multi-tenant support. Not enforced anywhere in v1 —
    // see LOE "Basic auth" notes. Every user currently gets the default tenant.
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId = DEFAULT_TENANT_ID;

    public static final UUID DEFAULT_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected User() {
        // JPA
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
