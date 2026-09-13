package com.community.waste.repo;

import com.community.waste.model.ReviewTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ReviewTaskRepo extends JpaRepository<ReviewTask, Long> {

    List<ReviewTask> findByStatusOrderByCreatedAtAsc(ReviewTask.Status status);

    List<ReviewTask> findByAssigneeIdAndStatusOrderByCreatedAtAsc(Long assigneeId, ReviewTask.Status status);

    List<ReviewTask> findByStatusAndDueAtBefore(ReviewTask.Status status, OffsetDateTime now);

    List<ReviewTask> findByDisposalId(Long disposalId);

    long countByStatusAndCreatedAtBetween(ReviewTask.Status status, OffsetDateTime from, OffsetDateTime to);
}
