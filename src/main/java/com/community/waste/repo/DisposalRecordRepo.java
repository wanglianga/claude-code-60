package com.community.waste.repo;

import com.community.waste.model.DisposalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface DisposalRecordRepo extends JpaRepository<DisposalRecord, Long> {

    List<DisposalRecord> findByUserIdOrderByDisposedAtDesc(Long userId);

    List<DisposalRecord> findByBucketPointIdAndDisposedAtAfter(Long bucketPointId, OffsetDateTime after);

    List<DisposalRecord> findByDisposedAtBetween(OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT COUNT(d) FROM DisposalRecord d WHERE d.bucketPoint.building.id = :buildingId AND d.disposedAt >= :from AND d.disposedAt < :to")
    long countByBuildingAndRange(@Param("buildingId") Long buildingId,
                                 @Param("from") OffsetDateTime from,
                                 @Param("to") OffsetDateTime to);

    @Query("SELECT COUNT(d) FROM DisposalRecord d WHERE d.bucketPoint.building.id = :buildingId AND d.status = 'REJECTED' AND d.disposedAt >= :from AND d.disposedAt < :to")
    long countMissortByBuildingAndRange(@Param("buildingId") Long buildingId,
                                        @Param("from") OffsetDateTime from,
                                        @Param("to") OffsetDateTime to);

    @Query("SELECT COUNT(d) FROM DisposalRecord d WHERE d.bucketPoint.id = :pointId AND d.disposedAt >= :from AND d.disposedAt < :to")
    long countByPointAndRange(@Param("pointId") Long pointId,
                              @Param("from") OffsetDateTime from,
                              @Param("to") OffsetDateTime to);

    @Query("SELECT COUNT(d) FROM DisposalRecord d WHERE d.bucketPoint.id = :pointId AND d.status = 'REJECTED' AND d.disposedAt >= :from AND d.disposedAt < :to")
    long countMissortByPointAndRange(@Param("pointId") Long pointId,
                                     @Param("from") OffsetDateTime from,
                                     @Param("to") OffsetDateTime to);

    @Query("SELECT COUNT(DISTINCT d.user.id) FROM DisposalRecord d WHERE d.bucketPoint.building.id = :buildingId AND d.disposedAt >= :from AND d.disposedAt < :to")
    long countDistinctUsersByBuildingAndRange(@Param("buildingId") Long buildingId,
                                              @Param("from") OffsetDateTime from,
                                              @Param("to") OffsetDateTime to);

    @Query("SELECT COUNT(DISTINCT d.user.id) FROM DisposalRecord d WHERE d.bucketPoint.id = :pointId AND d.disposedAt >= :from AND d.disposedAt < :to")
    long countDistinctUsersByPointAndRange(@Param("pointId") Long pointId,
                                          @Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(d.weightKg), 0) FROM DisposalRecord d WHERE d.bucketPoint.id = :pointId AND d.disposedAt > :since")
    BigDecimal sumWeightByPointSince(@Param("pointId") Long pointId, @Param("since") OffsetDateTime since);

    @Query("SELECT COALESCE(SUM(d.weightKg), 0) FROM DisposalRecord d WHERE d.disposedAt >= :from AND d.disposedAt < :to")
    BigDecimal sumWeightByRange(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(d.weightKg), 0) FROM DisposalRecord d WHERE d.bucketPoint.building.id = :buildingId AND d.disposedAt >= :from AND d.disposedAt < :to")
    BigDecimal sumWeightByBuildingAndRange(@Param("buildingId") Long buildingId,
                                           @Param("from") OffsetDateTime from,
                                           @Param("to") OffsetDateTime to);

    @Query("SELECT d.category AS category, COUNT(d) AS cnt, COALESCE(SUM(d.weightKg),0) AS weight " +
            "FROM DisposalRecord d WHERE d.disposedAt >= :from AND d.disposedAt < :to GROUP BY d.category")
    List<Object[]> statsByCategoryAndRange(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    long countByDisposedAtBetween(OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT COUNT(d) FROM DisposalRecord d WHERE d.status = 'REJECTED' AND d.disposedAt >= :from AND d.disposedAt < :to")
    long countMissortByRange(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT COUNT(DISTINCT d.user.id) FROM DisposalRecord d WHERE d.disposedAt >= :from AND d.disposedAt < :to")
    long countDistinctUsersByRange(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
