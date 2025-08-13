package com.ssafy.ottereview.pullrequest.service;

import com.ssafy.ottereview.account.service.UserAccountService;
import com.ssafy.ottereview.description.entity.Description;
import com.ssafy.ottereview.description.repository.DescriptionRepository;
import com.ssafy.ottereview.githubapp.client.GithubApiClient;
import com.ssafy.ottereview.githubapp.dto.GithubPrResponse;
import com.ssafy.ottereview.priority.entity.Priority;
import com.ssafy.ottereview.priority.repository.PriorityRepository;
import com.ssafy.ottereview.pullrequest.dto.info.PullRequestCommitInfo;
import com.ssafy.ottereview.pullrequest.dto.info.PullRequestFileInfo;
import com.ssafy.ottereview.preparation.dto.DescriptionInfo;
import com.ssafy.ottereview.preparation.dto.PriorityInfo;
import com.ssafy.ottereview.preparation.dto.UserInfo;
import com.ssafy.ottereview.pullrequest.dto.request.PullRequestCreateRequest;
import com.ssafy.ottereview.pullrequest.dto.response.PullRequestDetailResponse;
import com.ssafy.ottereview.pullrequest.dto.response.PullRequestResponse;
import com.ssafy.ottereview.pullrequest.entity.PullRequest;
import com.ssafy.ottereview.pullrequest.repository.PullRequestRepository;
import com.ssafy.ottereview.pullrequest.util.PullRequestMapper;
import com.ssafy.ottereview.repo.entity.Repo;
import com.ssafy.ottereview.repo.repository.RepoRepository;
import com.ssafy.ottereview.reviewer.entity.Reviewer;
import com.ssafy.ottereview.reviewer.repository.ReviewerRepository;
import com.ssafy.ottereview.user.entity.CustomUserDetail;
import com.ssafy.ottereview.user.entity.User;
import com.ssafy.ottereview.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class PullRequestServiceImpl implements PullRequestService {
    
    private final GithubApiClient githubApiClient;
    private final PullRequestRepository pullRequestRepository;
    private final RepoRepository repoRepository;
    private final UserRepository userRepository;
    private final ReviewerRepository reviewerRepository;
    private final UserAccountService userAccountService;
    private final PriorityRepository priorityRepository;
    private final DescriptionRepository descriptionRepository;
    private final PullRequestMapper pullRequestMapper;
    
    @Override
    public List<PullRequestResponse> getPullRequests(CustomUserDetail customUserDetail, Long repoId) {
        // 1. 사용자 권한 검증 및 레포지토리 조회
        Repo targetRepo = userAccountService.validateUserPermission(customUserDetail.getUser()
                .getId(), repoId);
        
        // 2. 해당 레포지토리의 Pull Request 목록 조회
        List<PullRequest> pullRequests = pullRequestRepository.findAllByRepo(targetRepo);
        
        log.info("여기까지 오는지 테스트")
        // 3. Pull Request 목록을 DTO로 변환하여 반환
        return pullRequests.stream()
                .map(pullRequestMapper::PullRequestToResponse)
                .toList();
    }
    
    @Override
    public List<PullRequestResㄴnse> getPullRequestsByGithub(CustomUserDetail userDetail,
            Long repoId) {
        
        Repo targetRepo = userAccountService.validateUserPermission(userDetail.getUser()
                .getId(), repoId);
        
        log.info("여기까지 오는지 테스트")
        // 2. Repo 엔티티에서 GitHub 저장소 이름과 설치 ID를 가져온다.
        String repositoryFullName = targetRepo.getFullName();
        Long installationId = targetRepo.getAccount()
                .getInstallationId();
        
        // 3. GitHub API를 호출하여 해당 레포지토리의 Pull Request 목록을 가져온다.
        List<GithubPrResponse> githubPrResponses = githubApiClient.getPullRequests(installationId,
                repositoryFullName);
        
        // 4~8. GitHub PR과 DB PR 동기화
        synchronizePullRequestsWithGithub(githubPrResponses, targetRepo, userDetail.getUser());
        
        // 9. 최종 결과 조회 및 반환 (삭제된 PR 제외)
        List<PullRequest> finalPullRequests = pullRequestRepository.findAllByRepo(targetRepo);
        
        return finalPullRequests.stream()
                .map(pullRequestMapper::PullRequestToResponse)
                .toList();
    }
    
    @Override
    public List<PullRequestResponse> getMyPullRequests(CustomUserDetail customUserDetail) {
        User loginUser = userRepository.findById(customUserDetail.getUser()
                        .getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + customUserDetail.getUser()
                        .getId()));
        
        List<PullRequest> pullRequests = pullRequestRepository.findAllByAuthor(loginUser);
        
        return pullRequests.stream()
                .map(pullRequestMapper::PullRequestToResponse)
                .toList();
    }
    
    @Override
    public PullRequestDetailResponse getPullRequestById(CustomUserDetail customUserDetail,
            Long repoId, Long pullRequestId) {
        
        userAccountService.validateUserPermission(customUserDetail.getUser()
                .getId(), repoId);
        
        PullRequest pullRequest = pullRequestRepository.findById(pullRequestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pull Request not found with id: " + pullRequestId));
        
        Long installationId = pullRequest.getRepo()
                .getAccount()
                .getInstallationId();
        // 파일변환 목록 가져오기
        List<PullRequestFileInfo> pullRequestFileChanges = githubApiClient.getPullRequestFileChanges(
                installationId, pullRequest.getRepo()
                        .getFullName(), pullRequest.getGithubPrNumber());
        
        // commit 목록 가져오기
        List<PullRequestCommitInfo> pullRequestCommitInfos = githubApiClient.getPullRequestCommits(
                installationId, pullRequest.getRepo()
                        .getFullName(), pullRequest.getGithubPrNumber());
        
        // dto 변환 후 리턴
        return pullRequestMapper.PullRequestToDetailResponse(pullRequest, pullRequestFileChanges,
                pullRequestCommitInfos);
        
    }
    
    @Override
    public void createPullRequest(CustomUserDetail customUserDetail, Long repoId,
            PullRequestCreateRequest request) {
        
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String action = jsonNode.path("action").asText();
            log.debug("[웹훅 이벤트 수신] 이벤트: {}, Action: {}", event, action);
            
        } catch (Exception e) {
            log.error("Error parsing payload: {}", e.getMessage());
        }
        
        Repo repo = userAccountService.validateUserPermission(customUserDetail.getUser()
                .getId(), repoId);
        
        // PR 생성 검증
        validatePullRequestCreation(request, repo);
        
        // PR 생성
        GithubPrResponse prResponse = GithubPrResponse.from(githubApiClient.createPullRequest(repo.getAccount()
                .getInstallationId(), repo.getFullName(), request.getTitle(), request.getBody(), request.getSource(), request.getTarget())
        );
        
        // PR 저장
        PullRequest pullRequest = pullRequestMapper.githubPrResponseToEntity(prResponse, customUserDetail.getUser(), repo);
        pullRequest.enrollSummary(request.getSummary());
        PullRequest savePullRequest = pullRequestRepository.save(pullRequest);
        
        // 리뷰어 저장
        List<UserInfo> reviewers = request.getReviewers();
        
        List<User> userList = reviewers.stream()
                .map(this::getUserFromUserInfo)
                .toList();
        
        List<Reviewer> reviewerList = userList.stream()
                .map(user -> Reviewer.builder()
                        .pullRequest(savePullRequest)
                        .user(user)
                        .build())
                .toList();
        
        reviewerRepository.saveAll(reviewerList);
        log.debug("리뷰어 저장 완료, 리뷰어 수: {}", reviewerList.size());
        
        // 우선 순위 저장
        List<PriorityInfo> priorities = request.getPriorities();
        List<Priority> priorityList = priorities.stream()
                .map((priorityInfo) -> Priority.builder()
                        .pullRequest(savePullRequest)
                        .title(priorityInfo.getTitle())
                        .idx(priorityInfo.getIdx())
                        .content(priorityInfo.getContent())
                        .build())
                .toList();
        
        priorityRepository.saveAll(priorityList);
        log.debug("우선 순위 저장 완료, 우선 순위 수: {}", priorityList.size());
        
        // 작성자 설명 저장
        List<DescriptionInfo> descriptions = request.getDescriptions();
        List<Description> descriptionList = descriptions.stream()
                .map(descriptionInfo ->
                        Description.builder()
                                .pullRequest(savePullRequest)
                                .path(descriptionInfo.getPath())
                                .position(descriptionInfo.getPosition())
                                .recordKey(descriptionInfo.getRecordKey())
                                .build()
                )
                .toList();
        
        descriptionRepository.saveAll(descriptionList);
        log.debug("설명 저장 완료, 설명 수: {}", descriptionList.size());
        
    }
    
    @Override
    public void createPullRequestFromGithub(List<GHRepository> githubRepositories) {
        
        try {
            //6. Repo 별 PR 생성
            for (GHRepository repo : githubRepositories) {
                log.info("Repo ID: {}, Full Name: {}, Private: {}",
                        repo.getId(), repo.getFullName(), repo.isPrivate());
                
                List<GithubPrResponse> githubPrResponses = repo.queryPullRequests()
                        .state(GHIssueState.OPEN)
                        .list()
                        .toList()
                        .stream()
                        .map(GithubPrResponse::from)
                        .toList();
                
                // 1. repositoryId로 Repo 엔티티를 조회한다.
                Repo targetRepo = repoRepository.findByRepoId(repo.getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Repository not found with id: " + repo.getId()));
                
                // 2. GitHub PR 응답을 PullRequest 엔티티로 변환
                List<PullRequest> newPullRequests = new ArrayList<>();
                List<Reviewer> newReviewers = new ArrayList<>();
                for (GithubPrResponse githubPr : githubPrResponses) {
                    
                    log.info("github title: {}", githubPr.getTitle());
                    
                    log.info("github id: {} ", githubPr.getAuthor()
                            .getId());
                    log.info("github name: {}", githubPr.getAuthor()
                            .getName());
                    log.info("github email: {}", githubPr.getAuthor()
                            .getEmail());
                    log.info("github Login: {}", githubPr.getAuthor()
                            .getLogin());
                    log.info("github type: {}", githubPr.getAuthor()
                            .getType());
                    
                    User author = userRepository.findByGithubId(githubPr.getAuthor()
                                    .getId())
                            .orElseGet(() -> registerUser(githubPr.getAuthor()));
                    
                    PullRequest pullRequest = pullRequestMapper.githubPrResponseToEntity(githubPr, author, targetRepo);
                    
                    List<GHUser> users = githubPr.getRequestedReviewers();
                    for (GHUser user : users) {
                        User reviewer = userRepository.findByGithubId(user
                                        .getId())
                                .orElseGet(() -> registerUser(user));
                        
                        log.info("reviewer 추가 로직, 이름: " + user.getName() + "pullrequest Id: "
                                + pullRequest.getId());
                        
                        newReviewers.add(
                                Reviewer.builder()
                                        .pullRequest(pullRequest)
                                        .user(reviewer)
                                        .build());
                    }
                    newPullRequests.add(pullRequest);
                }
                
                if (!newPullRequests.isEmpty()) {
                    pullRequestRepository.saveAll(newPullRequests);
                    reviewerRepository.saveAll(newReviewers);
                }
            }
        } catch (Exception e) {
            log.error("Error while creating pull requests from GitHub repositories: {}",
                    e.getMessage(), e);
            throw new RuntimeException("Failed to create pull requests from GitHub repositories",
                    e);
        }
        
    }
    
    private User registerUser(GHUser ghUser) {
        try {
            User user = User.builder()
                    .githubId(ghUser.getId())
                    .githubUsername(ghUser.getLogin())
                    .githubEmail(ghUser.getEmail() != null ? ghUser.getEmail() : null)
                    .type(ghUser.getType())
                    .profileImageUrl(ghUser.getAvatarUrl() != null ? ghUser.getAvatarUrl()
                            .toString() : null)
                    .rewardPoints(0)
                    .userGrade("BASIC")
                    .build();
            
            log.info("New user registered: {}", user.getGithubUsername());
            return userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to register user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register user from GitHub", e);
        }
    }
    
    private User getUserFromUserInfo(UserInfo userInfo) {
        return userRepository.findById(userInfo.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with id: " + userInfo.getId()));
    }
    
    /**
     * pullRequest 생성 시 필요한 정보가 모두 있는지 검증하는 메서드
     */
    private void validatePullRequestCreation(PullRequestCreateRequest pullRequestCreateRequest, Repo repo) {
        if (pullRequestCreateRequest.getSource() == null || pullRequestCreateRequest.getTarget() == null || pullRequestCreateRequest.getTitle() == null || repo.getFullName() == null) {
            throw new IllegalArgumentException("Source or target branch is null");
        }
    }
    
    /**
     * GitHub에서 가져온 PR 정보와 DB의 PR을 동기화하는 메서드
     *
     * @param githubPrResponses GitHub에서 가져온 PR 목록
     * @param targetRepo        대상 저장소
     * @param user              사용자 정보
     */
    private void synchronizePullRequestsWithGithub(List<GithubPrResponse> githubPrResponses,
            Repo targetRepo, User user) {
        
        List<PullRequest> existingPullRequests = pullRequestRepository.findAllByRepo(targetRepo);
        
        Map<Integer, PullRequest> existingPrMap = existingPullRequests.stream()
                .collect(Collectors.toMap(PullRequest::getGithubPrNumber, pr -> pr));
        
        // 5. GitHub에서 가져온 PR 번호 Set
        Set<Integer> githubPrNumbers = githubPrResponses.stream()
                .map(GithubPrResponse::getGithubPrNumber)
                .collect(Collectors.toSet());
        
        // 6. 새로운 PR과 기존 PR을 비교하여 저장할 Pull Request 목록을 준비한다.
        List<PullRequest> pullRequestsToSave = new ArrayList<>();
        
        for (GithubPrResponse githubPr : githubPrResponses) {
            PullRequest existingPr = existingPrMap.get(githubPr.getGithubPrNumber());
            
            if (existingPr == null) {
                // 6-1. 새로운 PR: 생성
                PullRequest newPr = pullRequestMapper.githubPrResponseToEntity(githubPr, user, targetRepo);
                newPr.enrollRepo(targetRepo);
                pullRequestsToSave.add(newPr);
            } else {
                // 6-2. 기존 PR: 업데이트 (변경사항이 있는 경우만)
                if (existingPr.hasChangedFrom(githubPr)) {
                    existingPr.updateFromGithub(githubPr);
                    pullRequestsToSave.add(existingPr);
                }
            }
        }
        
        // 7. 깃허브에 없는 PR들을 삭제 처리한다.
        List<PullRequest> prsToMarkAsDeleted = existingPullRequests.stream()
                .filter(pr -> !githubPrNumbers.contains(pr.getGithubPrNumber()))
                .toList();
        
        pullRequestRepository.deleteAll(prsToMarkAsDeleted);
        
        if (!pullRequestsToSave.isEmpty()) {
            pullRequestRepository.saveAll(pullRequestsToSave);
            log.info("총 {}개의 PR이 동기화되었습니다.", pullRequestsToSave.size());
        }
    }
}
