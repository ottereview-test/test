package com.ssafy.ottereview.preparation.service;

import com.ssafy.ottereview.account.service.UserAccountService;
import com.ssafy.ottereview.githubapp.client.GithubApiClient;
import com.ssafy.ottereview.preparation.dto.CommitInfo;
import com.ssafy.ottereview.preparation.dto.DiffHunk;
import com.ssafy.ottereview.preparation.dto.FileChangeInfo;
import com.ssafy.ottereview.preparation.dto.PreparationResult;
import com.ssafy.ottereview.preparation.dto.RepoInfo;
import com.ssafy.ottereview.preparation.dto.UserInfo;
import com.ssafy.ottereview.preparation.dto.request.AdditionalInfoRequest;
import com.ssafy.ottereview.preparation.dto.request.PreparationValidationRequest;
import com.ssafy.ottereview.preparation.repository.PreparationRedisRepository;
import com.ssafy.ottereview.pullrequest.repository.PullRequestRepository;
import com.ssafy.ottereview.pullrequest.util.DiffUtil;
import com.ssafy.ottereview.repo.entity.Repo;
import com.ssafy.ottereview.user.entity.CustomUserDetail;
import com.ssafy.ottereview.user.entity.User;
import com.ssafy.ottereview.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHCompare.Commit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class PrService {

    private final UserRepository userRepository;
    private final GithubApiClient githubApiClient;
    private final DiffUtil diffUtil;
    private final UserAccountService userAccountService;
    private final PreparationRedisRepository pullRequestRedisService;
    private final PullRequestRepository pullRequestRepository;
    
    public PreparationResult getPreparePullRequestInfo(CustomUserDetail userDetail, Long repoId, String source, String target) {

        userAccountService.validateUserPermission(userDetail.getUser()
                .getId(), repoId);

        PreparationResult preparationResult = pullRequestRedisService.getPrepareInfo(repoId, source, target);

        if (preparationResult == null) {
            throw new IllegalArgumentException("준비된 Pull Request 정보가 없습니다.");
        }

        return preparationResult;
    }

    public PreparationResult validatePullRequest(CustomUserDetail userDetail, Long repoId, PreparationValidationRequest request) {

        // 1. 유저 권한 검증
        Repo repo = userAccountService.validateUserPermission(userDetail.getUser()
                .getId(), repoId);

        // 이미 있는 PR일 경우 예외 처리
        if (pullRequestRepository.findByRepoAndBaseAndHead(repo, request.getTarget(), request.getSource())
                .isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 Pull Request입니다.");
        }
        
        User author = userDetail.getUser();

        Long accountId = repo.getAccount()
                .getId();
        List<UserInfo> reviewers = userAccountService.getUsersByAccount(accountId)
                .stream()
                .filter(user -> !user.getId()
                        .equals(author.getId())) // author 제외
                .map(user -> UserInfo.builder()
                        .id(user.getId())
                        .githubUsername(user.getGithubUsername())
                        .githubEmail(user.getGithubEmail())
                        .build()
                )
                .toList();

        GHCompare compare = githubApiClient.getCompare(repo.getAccount()
                .getInstallationId(), repo.getFullName(), request.getTarget(), request.getSource());

        PreparationResult preparationResult = convertToPreparePullRequestResponse(author, repo, compare, request, reviewers);

        pullRequestRedisService.savePrepareInfo(repoId, preparationResult);

        return preparationResult;
    }

    public void enrollAdditionalInfo(CustomUserDetail userDetail, Long repoId, AdditionalInfoRequest request) {
        try {
            userAccountService.validateUserPermission(userDetail.getUser()
                    .getId(), repoId);

            // 1. 기존 데이터 조회
            PreparationResult prepareInfo = pullRequestRedisService.getPrepareInfo(repoId, request.getSource(), request.getTarget());

            // 2. 선택적 필드들 업데이트
            if (request.getTitle() != null) {
                prepareInfo.enrollTitle(request.getTitle());
            }

            if (request.getBody() != null) {
                prepareInfo.enrollBody(request.getBody());
            }
            if (request.getReviewers() != null && !request.getReviewers()
                    .isEmpty()) {
                List<UserInfo> userInfos = convertToReviewerInfos(request.getReviewers());
                prepareInfo.enrollReviewers(userInfos);
            }

            if (request.getSummary() != null && !request.getSummary()
                    .trim()
                    .isEmpty()) {
                prepareInfo.enrollSummary(request.getSummary());
            }

            if (request.getDescription() != null && !request.getDescription()
                    .isEmpty()) {
                prepareInfo.enrollDescriptions(request.getDescription());
            }

            if (request.getDescription() != null && !request.getDescription()
                    .isEmpty()) {
                prepareInfo.enrollDescriptions(request.getDescription());
            }

            if (request.getPriorities() != null && !request.getPriorities()
                    .isEmpty()) {
                prepareInfo.enrollPriorities(request.getPriorities());
            }

            // 4. Repository에서 저장
            pullRequestRedisService.updatePrepareInfo(repoId, prepareInfo);

        } catch (Exception e) {
            log.error("추가 정보 업데이트 실패", e);
            throw new RuntimeException("추가 정보 업데이트 실패", e);
        }
    }

    private List<UserInfo> convertToReviewerInfos(List<Long> reviewerIds) {
        // reviewerIds를 ReviewerInfo 객체로 변환하는 로직
        return reviewerIds.stream()
                .map(this::getReviewerInfoById)
                .collect(Collectors.toList());
    }

    private UserInfo getReviewerInfoById(Long reviewerId) {
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found with id: " + reviewerId));

        return UserInfo.builder()
                .id(reviewer.getId())
                .githubUsername(reviewer.getGithubUsername())
                .githubEmail(reviewer.getGithubEmail())
                .build();
    }

    private PreparationResult convertToPreparePullRequestResponse(User author, Repo repo, GHCompare compare, PreparationValidationRequest request, List<UserInfo> reviewers) {

        return PreparationResult.builder()
                .source(request.getSource())
                .target(request.getTarget())
                .url(compare.getUrl()
                        .toString())
                .htmlUrl(compare.getHtmlUrl()
                        .toString())
                .permalinkUrl(compare.getPermalinkUrl()
                        .toString())
                .diffUrl(compare.getDiffUrl()
                        .toString())
                .patchUrl(compare.getPatchUrl()
                        .toString())
                .aheadBy(compare.getAheadBy())
                .behindBy(compare.getBehindBy())
                .totalCommits(compare.getTotalCommits())
                .status(compare.getStatus())
                .baseCommit(convertToCommitInfo(compare.getBaseCommit()))
                .mergeBaseCommit(convertToCommitInfo(compare.getMergeBaseCommit()))
                .commits(convertCommitArray(compare.getCommits()))
                .files(convertToFileChanges(compare.getFiles()))
                .author(UserInfo.of(author.getId(), author.getGithubUsername(), author.getGithubEmail()))
                .repository(RepoInfo.of(repo.getId(), repo.getFullName()))
                .preReviewers(reviewers)
                .isPossible(isCreatePR(compare.getStatus()
                        .toString(), compare.getAheadBy(), compare.getTotalCommits()))
                .build();
    }

    private List<FileChangeInfo> convertToFileChanges(GHCommit.File[] changedFiles) {
        List<FileChangeInfo> fileChanges = new ArrayList<>();

        for (GHCommit.File file : changedFiles) {
            try {
                log.debug("파일 이름 PreviousFilename: {}", file.getFileName());
                // GHCommit.File 객체에서 기본 정보 추출
                FileChangeInfo.FileChangeInfoBuilder builder = FileChangeInfo.builder()
                        
                        .filename(file.getFileName())
                        .status(file.getStatus())
                        .additions(file.getLinesAdded())
                        .deletions(file.getLinesDeleted())
                        .rawUrl(file.getRawUrl())
                        .blobUrl(file.getBlobUrl())
                        .changes(file.getLinesChanged());

                String detailedPatch = file.getPatch();
                if (detailedPatch != null && !detailedPatch.isEmpty()) {
                    List<DiffHunk> diffHunks = diffUtil.parseDiffHunks(detailedPatch);
                    builder.patch(detailedPatch)
                            .diffHunks(diffHunks);

                }

                fileChanges.add(builder.build());

            } catch (Exception e) {
                log.error("Error processing file change: {}", file.getFileName(), e);
            }
        }

        return fileChanges;
    }

    private CommitInfo convertToCommitInfo(Commit commit) {
        if (commit == null) {
            return null;
        }

        try {
            return CommitInfo.builder()
                    .sha(commit.getSHA1())
                    .message(commit.getCommit()
                            .getMessage())
                    .authorName(commit.getCommit()
                            .getAuthor()
                            .getName())
                    .authorEmail(commit.getCommit()
                            .getAuthor()
                            .getEmail())
                    .authorDate(commit.getCommit()
                            .getAuthor()
                            .getDate()
                            .toString())
                    .committerName(commit.getCommit()
                            .getCommitter()
                            .getName())
                    .committerEmail(commit.getCommit()
                            .getCommitter()
                            .getEmail())
                    .committerDate(commit.getCommit()
                            .getCommitter()
                            .getDate()
                            .toString())
                    .url(commit.getUrl()
                            .toString())
                    .htmlUrl(commit.getHtmlUrl()
                            .toString())
                    .additions(commit.getLinesAdded())
                    .deletions(commit.getLinesDeleted())
                    .totalChanges(commit.getLinesChanged())
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert commit info: " + e.getMessage(), e);
        }
    }

    private List<CommitInfo> convertCommitArray(Commit[] commits) {
        if (commits == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(commits)
                .map(this::convertToCommitInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    public Boolean isCreatePR(String status, int aheadBy, int totalCommits) {

        return switch (status.toLowerCase()) {
            case "ahead" -> aheadBy > 0;
            case "behind" -> totalCommits > 0;
            case "diverged" -> aheadBy > 0;
            case "identical" -> false;
            default -> {
                // 알 수 없는 상태
                System.out.println("Unknown status: " + status);
                yield false;
            }
        };
    }
}
