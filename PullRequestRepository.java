package com.ssafy.ottereview.pullrequest.repository;

import com.ssafy.ottereview.pullrequest.entity.PullRequest;
import com.ssafy.ottereview.repo.entity.Repo;
import com.ssafy.ottereview.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {

    List<PullRequest> findAllByRepo(Repo repo);

    List<PullRequest> findAllByAuthor(User author);

    Optional<PullRequest> findByGithubId(Long githubId);
}
