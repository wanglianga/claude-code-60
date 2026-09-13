package com.community.waste.repo;

import com.community.waste.model.CollectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CollectionRecordRepo extends JpaRepository<CollectionRecord, Long> {

    List<CollectionRecord> findByBucketPointIdOrderByScheduledAtDesc(Long bucketPointId);

    List<CollectionRecord> findByArrivedAtIsNullAndScheduledAtBefore(OffsetDateTime now);

    Optional<CollectionRecord> findFirstByBucketPointIdAndArrivedAtIsNotNullOrderByArrivedAtDesc(Long bucketPointId);

    List<CollectionRecord> findByScheduledAtBetween(OffsetDateTime from, OffsetDateTime to);
}
