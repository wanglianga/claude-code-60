package com.community.waste.repo;

import com.community.waste.model.RedemptionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface RedemptionOrderRepo extends JpaRepository<RedemptionOrder, Long> {

    List<RedemptionOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RedemptionOrder> findByStatusOrderByCreatedAtAsc(RedemptionOrder.Status status);

    @Query("SELECT COALESCE(SUM(o.quantity),0) FROM RedemptionOrder o WHERE o.user.id = :userId AND o.product.id = :productId AND o.status IN :statuses AND o.createdAt >= :since")
    long sumQuantityByUserAndProductSince(@Param("userId") Long userId,
                                          @Param("productId") Long productId,
                                          @Param("statuses") Collection<RedemptionOrder.Status> statuses,
                                          @Param("since") OffsetDateTime since);

    @Query("SELECT COALESCE(SUM(o.quantity),0) FROM RedemptionOrder o WHERE o.family.id = :familyId AND o.product.id = :productId AND o.status IN :statuses AND o.createdAt >= :since")
    long sumQuantityByFamilyAndProductSince(@Param("familyId") Long familyId,
                                            @Param("productId") Long productId,
                                            @Param("statuses") Collection<RedemptionOrder.Status> statuses,
                                            @Param("since") OffsetDateTime since);

    long countByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(o.pointsSpent),0) FROM RedemptionOrder o WHERE o.createdAt >= :from AND o.createdAt < :to AND o.status <> 'CANCELLED'")
    long sumPointsSpentByRange(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
