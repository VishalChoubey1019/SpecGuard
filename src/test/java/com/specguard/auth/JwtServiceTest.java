package com.specguard.auth;

import com.specguard.config.SpecGuardProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // >= 256 bits, required for HS256 (see JwtService's constructor comment).
    private static final String SECRET = "test-secret-key-at-least-32-bytes-long-for-hs256";

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("dev@example.com", "irrelevant-hash-not-used-here");
        setId(user, UUID.randomUUID());
    }

    @Test
    void issueToken_thenParseClaims_roundTripsEmailAndTenantId() {
        JwtService jwtService = jwtServiceWithExpiration(15);

        String token = jwtService.issueToken(user);
        var claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo(user.getEmail());
        assertThat(claims.get("tenant_id", String.class)).isEqualTo(user.getTenantId().toString());
    }

    @Test
    void extractUserId_returnsTheIdEncodedAtIssueTime() {
        JwtService jwtService = jwtServiceWithExpiration(15);

        String token = jwtService.issueToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    void parseClaims_throwsExpiredJwtException_onAlreadyExpiredToken() {
        // Negative expiration puts the "expiration" claim in the past the instant it's issued.
        JwtService jwtService = jwtServiceWithExpiration(-1);

        String token = jwtService.issueToken(user);

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseClaims_throwsJwtException_onGarbageToken() {
        JwtService jwtService = jwtServiceWithExpiration(15);

        assertThatThrownBy(() -> jwtService.parseClaims("not.a.real.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseClaims_throwsSignatureException_whenSignedWithDifferentSecret() {
        JwtService issuer = jwtServiceWithExpiration(15);
        JwtService verifier = jwtServiceWithSecret("a-completely-different-secret-key-32-bytes-plus-req-bits-is-384");

        String token = issuer.issueToken(user);

        assertThatThrownBy(() -> verifier.parseClaims(token))
                .isInstanceOf(SignatureException.class);
    }

    private static JwtService jwtServiceWithExpiration(long expirationMinutes) {
        return jwtServiceWith(SECRET, expirationMinutes);
    }

    private static JwtService jwtServiceWithSecret(String secret) {
        return jwtServiceWith(secret, 15);
    }

    private static JwtService jwtServiceWith(String secret, long expirationMinutes) {
        SpecGuardProperties props = new SpecGuardProperties();
        props.setSecret(secret);
        props.setExpirationMinutes(expirationMinutes);
        return new JwtService(props);
    }

    private static void setId(User user, UUID id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set test User id via reflection", e);
        }
    }
}