package com.specguard.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specguard.config.GithubProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives GitHub webhook deliveries. Authenticated via HMAC signature
 * ({@link GithubWebhookVerifier}), not JWT — see {@code SecurityConfig}, which
 * permits {@code /api/webhooks/**} without a bearer token for that reason.
 */
@RestController
@RequestMapping("/api/webhooks/github")
@Slf4j
@RequiredArgsConstructor
public class GithubWebhookController {

    private final GithubWebhookVerifier verifier;
    private final GithubProperties properties;
    private final PullRequestService pullRequestService;
    private final ObjectMapper objectMapper;

    /**
     * Entry point for all GitHub webhook deliveries. Only {@code pull_request} events are
     * currently handled; other event types are acknowledged but ignored.
     *
     * @param eventType     value of the {@code X-GitHub-Event} header (e.g. "pull_request", "ping")
     * @param signature     value of the {@code X-Hub-Signature-256} header
     * @param rawPayload    the raw request body, needed as-received for signature verification
     */
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String rawPayload) {

        if (!verifier.isValid(rawPayload, signature, properties.getWebhookSecret())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!"pull_request".equals(eventType)) {
            log.debug("Ignoring webhook event type: {}", eventType);
            return ResponseEntity.ok().build();
        }

        try {
            log.info("Received GitHub pull_request webhook");

            GithubPullRequestEvent event =
            objectMapper.readValue(rawPayload, GithubPullRequestEvent.class);

            log.info("PR received for repository");

            pullRequestService.handleEvent(event);

            log.info("Finished processing PR");
        } catch (Exception e) {
            // Deliberately swallow and 200: GitHub retries on non-2xx, and a malformed/unexpected
            // payload won't fix itself on retry. Logging is the recovery path for now.
            // TODO(phase-later): dead-letter malformed payloads somewhere inspectable instead of just logging.
            log.error("Failed to process pull_request webhook event", e);
        }

        return ResponseEntity.ok().build();
    }
}