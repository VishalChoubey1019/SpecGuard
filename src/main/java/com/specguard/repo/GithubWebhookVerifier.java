package com.specguard.repo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Verifies the {@code X-Hub-Signature-256} header GitHub sends on every webhook delivery,
 * proving the request actually came from GitHub and wasn't forged.
 *
 * @see <a href="https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries">
 *      GitHub docs: validating webhook deliveries</a>
 */
@Component
@Slf4j
public class GithubWebhookVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    /**
     * @param rawPayload      the exact raw request body bytes GitHub signed (must not be
     *                        re-serialized JSON, which can differ byte-for-byte from the original)
     * @param signatureHeader value of the {@code X-Hub-Signature-256} header
     * @param secret          the shared webhook secret configured on both GitHub and here
     * @return true if the signature is valid for this payload and secret
     */
    public boolean isValid(String rawPayload, String signatureHeader, String secret) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Webhook rejected: missing or malformed X-Hub-Signature-256 header");
            return false;
        }

        String expectedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());
        String actualHex = computeHmacHex(rawPayload, secret);

        // Constant-time compare: a naive String.equals() leaks timing info an attacker
        // could use to guess the signature byte-by-byte.
        boolean valid = MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.UTF_8),
                actualHex.getBytes(StandardCharsets.UTF_8));

        if (!valid) {
            log.warn("Webhook rejected: signature mismatch");
        }
        return valid;
    }

    private String computeHmacHex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            // HmacSHA256 is a standard JDK algorithm; this only fails on a broken JVM install.
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}