package com.specguard.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Persistence access for {@link PullRequest}. */
public interface PullRequestRepo extends JpaRepository<PullRequest, UUID> {

    /**
     * @param repoFullName GitHub {@code org/repo} slug
     * @param prNumber     per-repo PR number
     * @return the existing tracked PR row, if the webhook has seen this PR before
     */
    Optional<PullRequest> findByRepoFullNameAndPrNumber(String repoFullName, int prNumber);
}