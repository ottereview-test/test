# test
test

2025-08-06T16:05:28.406+09:00 DEBUG 58060 --- [ottereview] [nio-8080-exec-5] c.s.o.w.service.ReviewEventService       : 리뷰 생성 호출
Hibernate: 
    select
        pr1_0.id,
        pr1_0.approve_cnt,
        pr1_0.user_id,
        pr1_0.base,
        pr1_0.body,
        pr1_0.changed_files_cnt,
        pr1_0.comment_cnt,
.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정
        pr1_0.created_at,
        pr1_0.diff_url,
        pr1_0.github_created_at,
        pr1_0.github_id,
        pr1_0.github_pr_number,
        pr1_0.github_updated_at,
        pr1_0.head,
        pr1_0.html_url,
        pr1_0.issue_url,
        pr1_0.mergeable,
        pr1_0.merged,
        pr1_0.modified_at,
.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정
Hibernate: 
    select
        u1_0.id,
        u1_0.github_email,
        u1_0.github_id,
        u1_0.github_username,
        u1_0.profile_image_url,
        u1_0.reward_points,
        u1_0.type,
        u1_0.user_grade 
    from
        users u1_0 
    where
        u1_0.github_id=?
Hibernate: 
    insert 
    into
        review
        (body, commit_sha, created_at, github_created_at, github_id, modified_at, pull_request_id, state, user_id) 
    values
        (?, ?, ?, ?, ?, ?, ?, ?, ?)
2025-08-06T16:05:28.582+09:00  INFO 58060 --- [ottereview] [nio-8080-exec-6] c.s.o.w.c.GithubWebhookController        : [웹훅 이벤트 수신] 이벤트: pull_request_review, delivery: bb48fe5a-7293-11f0-91d9-759c606ad728
2025-08-06T16:05:28.582+09:00  INFO 58060 --- [ottereview] [nio-8080-exec-6] c.s.o.w.c.GithubWebhookController        : Handling pull request review event
2025-08-06T16:05:28.585+09:00 DEBUG 58060 --- [ottereview] [nio-8080-exec-6] c.s.o.w.service.ReviewEventService       : 리뷰어가 처음으로 리뷰를 제출할 때 발생하는 callback
Hibernate: 
    select
        r1_0.id,
        r1_0.body,
        r1_0.commit_sha,
.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정
    from
        review r1_0 
    where
        r1_0.github_id=?
2025-08-06T16:05:28.593+09:00 DEBUG 58060 --- [ottereview] [nio-8080-exec-6] c.s.o.w.service.ReviewEventService       : 업데이트 리뷰 호출
2025-08-06T16:05:28.743+09:00  INFO 58060 --- [ottereview] [nio-8080-exec-7] c.s.o.w.c.GithubWebhookController        : [웹훅 이벤트 수신] 이벤트: pull_request_review_comment, delivery: bb5a1f50-7293-11f0-9bd6-32d3fc9a1cd6
2025-08-06T16:05:28.743+09:00  INFO 58060 --- [ottereview] [nio-8080-exec-7] c.s.o.w.c.GithubWebhookController        : Handling pull request review event
2025-08-06T16:05:28.753+09:00 DEBUG 58060 --- [ottereview] [nio-8080-exec-7] c.s.o.w.s.ReviewCommentEventService      : DTO로 받은 ReviewCommentEventDto event: {
  "action" : "created",
  "comment" : {
    "id" : 2256074214,
    "path" : "test.txt",
    "user" : {
      "login" : "kangboom",
      "id" : 103165796,
      "avatar_url" : "https://avatars.githubusercontent.com/u/103165796?v=4",
      "html_url" : "https://github.com/kangboom"
    },
    "body" : "리뷰 코멘트",
    "line" : 12,
    "side" : "RIGHT",
    "position" : 7,
    "pull_request_review_id" : 3090708362,
    "node_id" : "PRRC_kwDOPYZagM6GePXm",
    "diff_hunk" : "@@ -8,18 +8,21 @@ Caused by: org.hibernate.NonUniqueResultException: Query did not return a unique\n \tat org.hibernate.query.spi.AbstractSelectionQuery.uniqueElement(AbstractSelectionQuery.java:298) ~[hibernate-core-6.6.18.Final.jar:6.6.18.Final]\n \tat org.hibernate.query.spi.AbstractSelectionQuery.getSingleResult(AbstractSelectionQuery.java:281) ~[hibernate-core-6.6.18.Final.jar:6.6.18.Final]\n \tat org.springframework.data.jpa.repository.query.JpaQueryExecution$SingleEntityExecution.doExecute(JpaQueryExecution.java:224) ~[spring-data-jpa-3.5.1.jar:3.5.1]\n-\tat org.springframework.data.jpa.repository.query.JpaQueryExecution.execute(JpaQueryExecution.java:93) ~[spring-data-jpa-3.5.1.jar:3.5.1]\n-\tat org.springframework.data.jpa.repository.query.AbstractJpaQuery.doExecute(AbstractJpaQuery.java:160) ~[spring-data-jpa-3.5.1.jar:3.5.1]\n+\t이 두문장은 삭제 되었습니다.\n+\t이 두문장은 삭제 되었습니다.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정
    "author_association" : "COLLABORATOR",
    "start_line" : 12,
    "original_start_line" : 12,
    "start_side" : "LEFT",
    "original_line" : 12,
    "original_position" : 7,
    "subject_type" : "line"
  }
}
.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정.",
    수정수정수정수정수정수정수정수정수정수정수정수정
