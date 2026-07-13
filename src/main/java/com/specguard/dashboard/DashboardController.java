package com.specguard.dashboard;

import com.specguard.dashboard.dto.PullRequestDto;
import com.specguard.dashboard.dto.ViolationDto;
import com.specguard.governance.Severity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** REST endpoints backing the dashboard's pull request table and violation drill-down. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** @return every pull request SpecGuard is tracking. */
    @GetMapping("/pull-requests")
    public ResponseEntity<List<PullRequestDto>> getPullRequests() {
        return ResponseEntity.ok(dashboardService.getPullRequests());
    }

    /** @return every recorded violation, across all pull requests. */
    @GetMapping("/violations")
    public ResponseEntity<List<ViolationDto>> getViolations() {
        return ResponseEntity.ok(dashboardService.getViolations());
    }

    /** @return the violations recorded against the given pull request. */
    @GetMapping("/pull-requests/{pullRequestId}/violations")
    public ResponseEntity<List<ViolationDto>> getViolationsForPullRequest(@PathVariable UUID pullRequestId) {
        return ResponseEntity.ok(dashboardService.getViolationsForPullRequest(pullRequestId));
    }

    // TODO: add an endpoint to filter violations by category too, mirroring this severity filter
    /** @return every recorded violation at the given severity, across all pull requests. */
    @GetMapping("/violations/severity/{severity}")
    public ResponseEntity<List<ViolationDto>> getViolationsBySeverity(@PathVariable Severity severity) {
        return ResponseEntity.ok(dashboardService.getViolationsBySeverity(severity));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
