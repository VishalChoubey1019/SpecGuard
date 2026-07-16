package com.specguard.repo;

import com.specguard.governance.Severity;
import com.specguard.governance.Violation;
import com.specguard.governance.ViolationCategory;
import com.specguard.governance.ViolationRepo;
import com.specguard.governance.openapi.OpenApiDiffAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PullRequestServiceTest {

    private static final String REPO = "vishal/specguard";
    private static final String BASE_SHA = "aaaaaaaa";
    private static final String HEAD_SHA = "bbbbbbbb";

    @Mock
    private PullRequestRepo pullRequestRepository;

    @Mock
    private GithubClient githubClient;

    @Mock
    private OpenApiDiffAnalyzer analyzer;

    @Mock
    private ViolationRepo violationRepository;

    private PullRequestService service;

    @BeforeEach
    void setUp() {
        service = new PullRequestService(
                pullRequestRepository,
                githubClient,
                analyzer,
                violationRepository);
    }

    @Test
    void openedEvent_createsPullRequest_andRunsAnalyzer() {

        GithubPullRequestEvent event = event("opened", false);

        when(pullRequestRepository.findByRepoFullNameAndPrNumber(REPO, 1))
                .thenReturn(Optional.empty());

        when(pullRequestRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        List<ChangedFile> files = List.of(
                new ChangedFile("openapi.yaml", "modified", 1, 0, "")
        );

        when(githubClient.fetchChangedFiles(REPO, 1))
                .thenReturn(files);

        when(analyzer.analyze(any(), eq(files)))
                .thenReturn(List.of());

        service.handleEvent(event);

        verify(pullRequestRepository).save(any(PullRequest.class));
        verify(githubClient).fetchChangedFiles(REPO, 1);
        verify(analyzer).analyze(any(PullRequest.class), eq(files));
        verifyNoInteractions(violationRepository);
    }

    @Test
    void synchronizeEvent_updatesExistingPullRequest() {

        PullRequest existing = new PullRequest(
                REPO,
                1,
                123L,
                "Old title",
                "vishal",
                "oldBase",
                "oldHead",
                PrStatus.OPEN);

        when(pullRequestRepository.findByRepoFullNameAndPrNumber(REPO, 1))
                .thenReturn(Optional.of(existing));

        when(pullRequestRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(githubClient.fetchChangedFiles(any(), anyInt()))
                .thenReturn(List.of());

        when(analyzer.analyze(any(), any()))
                .thenReturn(List.of());

        service.handleEvent(event("synchronize", false));

        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getBaseSha()).isEqualTo(BASE_SHA);
        assertThat(existing.getHeadSha()).isEqualTo(HEAD_SHA);

        verify(pullRequestRepository).save(existing);
    }

    @Test
    void nonAnalyzableAction_returnsEmpty_withoutCallingAnalyzer() {

        when(pullRequestRepository.findByRepoFullNameAndPrNumber(REPO, 1))
                .thenReturn(Optional.empty());

        when(pullRequestRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        List<Violation> result = service.handleEvent(event("labeled", false));

        assertThat(result).isEmpty();

        verifyNoInteractions(githubClient);
        verifyNoInteractions(analyzer);
        verifyNoInteractions(violationRepository);
    }

    @Test
    void violationsArePersisted() {

        when(pullRequestRepository.findByRepoFullNameAndPrNumber(REPO, 1))
                .thenReturn(Optional.empty());

        when(pullRequestRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        List<ChangedFile> files = List.of(
                new ChangedFile("openapi.yaml", "modified", 1, 0, "")
        );

        when(githubClient.fetchChangedFiles(REPO, 1))
                .thenReturn(files);

        Violation violation = new Violation(
                new PullRequest(
                        REPO,
                        1,
                        123L,
                        "New title",
                        "vishal",
                        BASE_SHA,
                        HEAD_SHA,
                        PrStatus.OPEN),
                ViolationCategory.API_CONTRACT,
                Severity.CRITICAL,
                "rule-1",
                "breaking change",
                "openapi.yaml"
        );

        when(analyzer.analyze(any(), eq(files)))
                .thenReturn(List.of(violation));

        service.handleEvent(event("opened", false));

        ArgumentCaptor<List<Violation>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(violationRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .containsExactly(violation);
    }

    @Test
    void closedMergedEvent_marksStatusMerged() {

        when(pullRequestRepository.findByRepoFullNameAndPrNumber(REPO, 1))
                .thenReturn(Optional.empty());

        when(pullRequestRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        service.handleEvent(event("closed", true));

        ArgumentCaptor<PullRequest> captor =
                ArgumentCaptor.forClass(PullRequest.class);

        verify(pullRequestRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus())
                .isEqualTo(PrStatus.MERGED);

        verifyNoInteractions(githubClient);
    }

    @Test
    void closedUnmergedEvent_marksStatusClosed() {

        when(pullRequestRepository.findByRepoFullNameAndPrNumber(REPO, 1))
                .thenReturn(Optional.empty());

        when(pullRequestRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        service.handleEvent(event("closed", false));

        ArgumentCaptor<PullRequest> captor =
                ArgumentCaptor.forClass(PullRequest.class);

        verify(pullRequestRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus())
                .isEqualTo(PrStatus.CLOSED);

        verifyNoInteractions(githubClient);
    }

    private GithubPullRequestEvent event(String action, boolean merged) {

        return new GithubPullRequestEvent(
                action,
                new GithubPullRequestEvent.PullRequestPayload(
                        123L,
                        1,
                        "New title",
                        new GithubPullRequestEvent.UserPayload("vishal"),
                        new GithubPullRequestEvent.RefPayload(BASE_SHA),
                        new GithubPullRequestEvent.RefPayload(HEAD_SHA),
                        merged
                ),
                new GithubPullRequestEvent.RepositoryPayload(REPO)
        );
    }
}