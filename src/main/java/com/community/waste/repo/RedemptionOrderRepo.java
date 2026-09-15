package com.community.waste.repo;

import com.community.waste.model.RedemptionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** 条件流转：仅 PENDING 可核销，返回 0 表示已被处理。 */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RedemptionOrder o SET o.status = 'FULFILLED', o.fulfilledAt = :now WHERE o.id = :id AND o.status = 'PENDING'")
    int fulfillIfPending(@Param("id") Long id, @Param("now") OffsetDateTime now);

    /** 条件流转：仅 PENDING 可取消，返回 0 表示已被处理（防重复退库存/退款）。 */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RedemptionOrder o SET o.status = 'CANCELLED' WHERE o.id = :id AND o.status = 'PENDING'")
    int cancelIfPending(@Param("id") Long id);
}
