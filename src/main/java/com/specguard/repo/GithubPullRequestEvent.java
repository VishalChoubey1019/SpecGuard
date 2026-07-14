package com.specguard.repo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of GitHub's {@code pull_request} webhook event payload that SpecGuard reads.
 * Deliberately maps only the fields currently used — GitHub's real payload has dozens
 * more, and Jackson ignores anything not declared here.
 *
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#pull_request">
 *      GitHub docs: pull_request event</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubPullRequestEvent(
        String action,
        @JsonProperty("pull_request") PullRequestPayload pullRequest,
        RepositoryPayload repository
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequestPayload(
            long id,
            int number,
            String title,
            UserPayload user,
            RefPayload base,
            RefPayload head,
            boolean merged
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserPayload(String login) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefPayload(String sha) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RepositoryPayload(@JsonProperty("full_name") String fullName) {}
}