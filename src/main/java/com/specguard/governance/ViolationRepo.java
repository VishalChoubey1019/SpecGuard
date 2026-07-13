package com.specguard.governance;

import com.specguard.repo.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Persistence access for {@link Violation}. */
public interface ViolationRepo extends JpaRepository<Violation, UUID> {

    /** @return every violation recorded for the given pull request, most recent analyzer runs included. */
    List<Violation> findByPullRequest(PullRequest pullRequest);

    /** @return every violation recorded at the given severity, across all pull requests. */
    List<Violation> findBySeverity(Severity severity);
}