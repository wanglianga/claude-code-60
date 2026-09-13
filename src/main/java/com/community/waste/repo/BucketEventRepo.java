package com.community.waste.repo;

import com.community.waste.model.BucketEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BucketEventRepo extends JpaRepository<BucketEvent, Long> {

    List<BucketEvent> findByStatusOrderByCreatedAtDesc(BucketEvent.Status status);

    List<BucketEvent> findByStatusNotOrderByCreatedAtDesc(BucketEvent.Status status);

    @Query("SELECT e FROM BucketEvent e WHERE e.dedupeKey = :dedupeKey AND e.status <> 'RESOLVED'")
    Optional<BucketEvent> findOpenByDedupeKey(@Param("dedupeKey") String dedupeKey);

    @Query("SELECT DISTINCT e FROM BucketEvent e JOIN e.participants p WHERE p.user.id = :userId ORDER BY e.createdAt DESC")
    List<BucketEvent> findByParticipantUserId(@Param("userId") Long userId);
}
