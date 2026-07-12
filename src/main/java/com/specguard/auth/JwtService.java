package com.specguard.auth;

import com.specguard.config.SpecGuardProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    // Kept as an explicit constructor rather than @RequiredArgsConstructor: it does real
    // work (deriving the signing key from the configured secret), not just field assignment,
    // so generating it would hide logic Lombok can't express.
    public JwtService(SpecGuardProperties props) {
        // HS256 needs >= 256 bits; the .env.example default is intentionally unsafe so this
        // fails loudly in dev if nobody replaced JWT_SECRET yet.
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = props.getExpirationMinutes();
        log.info("JwtService initialized, token expiration: {} minutes", expirationMinutes);
    }

    public String issueToken(User user) {
        // Captured once and reused for both issuedAt and expiration below, so the token's
        // lifetime is exactly expirationMinutes rather than drifting by the gap between
        // two separate Instant.now() calls.
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("tenant_id", user.getTenantId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key)
                .compact();
        log.debug("Issued JWT for user id: {}", user.getId());
        return token;
    }

    /** Throws io.jsonwebtoken.JwtException on invalid/expired tokens. */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    // TODO(phase-later): move JWT_SECRET rotation story into scope before any real multi-user
    // rollout — right now rotating the secret invalidates every outstanding token at once.
}
