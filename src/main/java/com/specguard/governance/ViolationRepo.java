package com.specguard.governance;

import com.specguard.repo.PullRequest;
import com.specguard.repo.PrStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Persistence access for {@link Violation}. 
 * Spring Data JPA derives queries from method names at application startup.
 * Invalid property paths fail with {@code PropertyReferenceException}.
 *
 * {@link Violation} has no direct {@code repoFullName} or {@code status} fields;
 * they belong to {@code violation.pullRequest}. 
 * 
 * Query across the relationship using
 * {@code findByPullRequest_RepoFullName(...)} (or the equivalent nested form).
 *
 * Rule: when querying related entities, use
 * {@code findBy<Relation>_<Field>} matching the actual entity fields.
*/
public interface ViolationRepo extends JpaRepository<Violation, UUID> {

    /** @return every violation recorded for the given pull request, most recent analyzer runs included. */
    List<Violation> findByPullRequest(PullRequest pullRequest);

    /** @return every violation recorded at the given severity, across all pull requests. */
    List<Violation> findBySeverity(Severity severity);

    /** @return every violation recorded across all pull requests tracked for the given repo. */
    List<Violation> findByPullRequest_RepoFullName(String repoFullName);

    /** @return every violation recorded across pull requests in the given repo that are currently at the given status. */
    List<Violation> findByPullRequest_RepoFullNameAndPullRequest_Status(String repoFullName, PrStatus status);
}