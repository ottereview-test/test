package com.ssafy.ottereview.pullrequest.dto.info;

import java.net.URL;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.kohsuke.github.GHPullRequestCommitDetail;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PullRequestCommitInfo {

    // 기본 커밋 정보
    private String sha;                    // 커밋 해시
    private String message;                // 커밋 메시지
    private URL url;                    // GitHub 커밋 URL

    // 작성자 정보
    private String authorName;             // 작성자 이름
    private String authorEmail;            // 작성자 이메일
    private LocalDateTime authorDate;      // 작성 일시

    // 커미터 정보
    private String committerName;          // 커미터 이름
    private String committerEmail;         // 커미터 이메일
    private LocalDateTime committerDate;   // 커밋 일시

    /**
     * GHPullRequestCommitDetail로부터 PullRequestCommitDetail을 생성하는 팩토리 메서드
     *
     * @param ghCommitDetail GitHub Pull Request 커밋 상세 정보
     * @return PullRequestCommitDetail 객체
     */
    public static PullRequestCommitInfo from(GHPullRequestCommitDetail ghCommitDetail) {
        try {
            PullRequestCommitInfoBuilder builder = PullRequestCommitInfo.builder()
                    .sha(ghCommitDetail.getSha())
                    .url(ghCommitDetail.getUrl());

            // 커밋 정보 설정
            if (ghCommitDetail.getCommit() != null) {
                builder.message(ghCommitDetail.getCommit().getMessage());

                // 작성자 정보
                if (ghCommitDetail.getCommit().getAuthor() != null) {
                    builder.authorName(ghCommitDetail.getCommit().getAuthor().getName())
                            .authorEmail(ghCommitDetail.getCommit().getAuthor().getEmail())
                            .authorDate(convertToLocalDateTime(ghCommitDetail.getCommit().getAuthor().getDate()));
                }

                // 커미터 정보
                if (ghCommitDetail.getCommit().getCommitter() != null) {
                    builder.committerName(ghCommitDetail.getCommit().getCommitter().getName())
                            .committerEmail(ghCommitDetail.getCommit().getCommitter().getEmail())
                            .committerDate(convertToLocalDateTime(ghCommitDetail.getCommit().getCommitter().getDate()));
                }
            }

            return builder.build();

        } catch (Exception e) {
            throw new RuntimeException("GitHub 커밋 정보를 변환하는 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 날짜 변환 유틸리티 메서드
     *
     * @param dateObj 변환할 날짜 객체
     * @return LocalDateTime 객체, 변환 실패시 현재 시간
     */
    private static LocalDateTime convertToLocalDateTime(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        try {
            // GitHub API는 java.util.Date를 반환
            if (dateObj instanceof java.util.Date) {
                java.util.Date date = (java.util.Date) dateObj;
                return date.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            }

            // 만약 다른 타입이라면 toString()으로 파싱 시도
            String dateStr = dateObj.toString();
            return LocalDateTime.parse(dateStr.replace("Z", ""));

        } catch (Exception e) {
            // 변환 실패시 현재 시간 반환
            return LocalDateTime.now();
        }
    }

    /**
     * 커밋 메시지의 첫 번째 줄만 반환하는 유틸리티 메서드
     *
     * @return 커밋 제목 (첫 번째 줄)
     */
    public String getCommitTitle() {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }

        String[] lines = message.split("\n");
        return lines[0].trim();
    }

    /**
     * 커밋 메시지의 본문 부분을 반환하는 유틸리티 메서드
     *
     * @return 커밋 본문 (두 번째 줄 이후)
     */
    public String getCommitBody() {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }

        String[] lines = message.split("\n");
        if (lines.length <= 1) {
            return "";
        }

        StringBuilder body = new StringBuilder();
        for (int i = 1; i < lines.length; i++) {
            if (i > 1) body.append("\n");
            body.append(lines[i]);
        }
        return body.toString().trim();
    }

    /**
     * 짧은 SHA 해시를 반환하는 유틸리티 메서드
     *
     * @return 7자리 짧은 SHA 해시
     */
    public String getShortSha() {
        if (sha == null || sha.length() < 7) {
            return sha;
        }
        return sha.substring(0, 7);
    }
}
