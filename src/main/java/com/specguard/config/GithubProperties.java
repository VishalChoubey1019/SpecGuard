package com.specguard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds {@code specguard.github.*} config from environment variables (see .env.example).
 */
@Component
@ConfigurationProperties(prefix = "specguard.github")
@Data
public class GithubProperties {
    /*
     * Shared secret configured on the GitHub webhook, used to verify {@code X-Hub-Signature-256}. 
     * Used in GitHubWebhookController.receive()
     * 
    */
    private String webhookSecret;
    /** Token used to authenticate outbound calls to the GitHub REST API (fetching PR files, posting comments). */
    private String appToken;
}