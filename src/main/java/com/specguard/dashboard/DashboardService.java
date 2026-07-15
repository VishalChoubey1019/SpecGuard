package com.specguard.dashboard;

import com.specguard.dashboard.dto.PullRequestDto;
import com.specguard.dashboard.dto.ViolationDto;
import com.specguard.governance.Severity;
import com.specguard.governance.Violation;
import com.specguard.governance.ViolationRepo;
import com.specguard.repo.PullRequest;
import com.specguard.repo.PullRequestRepo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import lombok.NonNull;

/** Read-only aggregation of tracked pull requests and their violations for the dashboard. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PullRequestRepo pullRequestRepo;
    private final ViolationRepo violationRepo;

    /** @return every pull request SpecGuard is tracking. */
    public List<PullRequestDto> getPullRequests() {
        return pullRequestRepo.findAll().stream().map(DashboardService::toDto).toList();
    }

    /** @return every recorded violation, across all pull requests. */
    public List<ViolationDto> getViolations() {
        return violationRepo.findAll().stream().map(DashboardService::toDto).toList();
    }

    /**
     * @param pullRequestId id of a tracked pull request
     * @return the violations recorded against that pull request
     * @throws NoSuchElementException if no pull request with that id is tracked
     */
    public List<ViolationDto> getViolationsForPullRequest(UUID pullRequestId) {
        PullRequest pullRequest = pullRequestRepo.findById(Objects.requireNonNull(pullRequestId))
                .orElseThrow(() -> new NoSuchElementException("no pull request with id " + pullRequestId));
        return violationRepo.findByPullRequest(pullRequest).stream().map(DashboardService::toDto).toList();
    }

    /** @return every recorded violation at the given severity, across all pull requests. */
    public List<ViolationDto> getViolationsBySeverity(Severity severity) {
        return violationRepo.findBySeverity(severity).stream().map(DashboardService::toDto).toList();
    }

    private static PullRequestDto toDto(@NonNull PullRequest pr) {
        return new PullRequestDto(pr.getId(), pr.getRepoFullName(), pr.getPrNumber(), pr.getTitle(), pr.getStatus());
    }

    private static ViolationDto toDto(@NonNull Violation violation) {
        return new ViolationDto(violation.getId(), violation.getPullRequest().getId(), violation.getCategory(),
                violation.getSeverity(), violation.getRuleId(), violation.getMessage(), violation.getFilePath());
    }
}
