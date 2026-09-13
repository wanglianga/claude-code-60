package com.community.waste.repo;

import com.community.waste.model.PointsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface PointsTransactionRepo extends JpaRepository<PointsTransaction, Long> {

    List<PointsTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PointsTransaction> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    List<PointsTransaction> findByRefTypeAndRefId(String refType, Long refId);

    @Query("SELECT COALESCE(SUM(t.delta), 0) FROM PointsTransaction t WHERE t.type = :type AND t.createdAt >= :from AND t.createdAt < :to")
    long sumDeltaByTypeAndRange(@Param("type") PointsTransaction.TxType type,
                                @Param("from") OffsetDateTime from,
                                @Param("to") OffsetDateTime to);
}
