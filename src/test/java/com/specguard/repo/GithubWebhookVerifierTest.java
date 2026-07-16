package com.specguard.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;


import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GithubWebhookVerifierTest {

    private static final String SECRET = "local-test-secret";
    private static final String PAYLOAD = """
            {
              "action":"opened",
              "repository":{"full_name":"vishal/specguard"}
            }
            """;

    private GithubWebhookVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new GithubWebhookVerifier();
    }

    @Test
    void validSignature_returnsTrue() throws Exception {
        String signature = "sha256=" + hmac(PAYLOAD, SECRET);

        boolean valid = verifier.isValid(PAYLOAD, signature, SECRET);

        assertThat(valid).isTrue();
    }

    @Test
    void differentSecret_returnsFalse() throws Exception {
        String signature = "sha256=" + hmac(PAYLOAD, SECRET);

        boolean valid = verifier.isValid(
                PAYLOAD,
                signature,
                "wrong-secret");

        assertThat(valid).isFalse();
    }

    @Test
    void modifiedPayload_returnsFalse() throws Exception {
        String signature = "sha256=" + hmac(PAYLOAD, SECRET);

        boolean valid = verifier.isValid(
                PAYLOAD + " ",
                signature,
                SECRET);

        assertThat(valid).isFalse();
    }

    @Test
    void missingSignature_returnsFalse() {
        assertThat(verifier.isValid(PAYLOAD, null, SECRET))
                .isFalse();
    }

    @Test
    void malformedSignature_returnsFalse() {
        assertThat(verifier.isValid(
                PAYLOAD,
                "abcdef123456",
                SECRET))
                .isFalse();
    }

    @Test
    void wrongDigest_returnsFalse() {
        assertThat(verifier.isValid(
                PAYLOAD,
                "sha256=deadbeef",
                SECRET))
                .isFalse();
    }

    private static String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"));

        return HexFormat.of().formatHex(
                mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}