package com.community.waste.repo;

import com.community.waste.model.RedemptionReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface RedemptionReservationRepo extends JpaRepository<RedemptionReservation, Long> {

    List<RedemptionReservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RedemptionReservation> findByProductIdAndStatusOrderByCreatedAtAsc(Long productId,
                                                                            RedemptionReservation.Status status);

    List<RedemptionReservation> findByStatusOrderByCreatedAtAsc(RedemptionReservation.Status status);

    List<RedemptionReservation> findByStatusAndExpireAtBefore(RedemptionReservation.Status status, OffsetDateTime now);

    long countByStatus(RedemptionReservation.Status status);

    long countByStatusAndFulfilledAtBetween(RedemptionReservation.Status status, OffsetDateTime from, OffsetDateTime to);

    long countByStatusAndCancelledAtBetween(RedemptionReservation.Status status, OffsetDateTime from, OffsetDateTime to);

    /** 排队位次：排在该预约之前的等待人数 */
    @Query("SELECT COUNT(r) FROM RedemptionReservation r WHERE r.product.id = :productId AND r.status = 'WAITING' AND r.createdAt < :createdAt")
    long countWaitingAhead(@Param("productId") Long productId, @Param("createdAt") OffsetDateTime createdAt);

    /** 本月某用户某商品的预约占用量（计入限购） */
    @Query("SELECT COALESCE(SUM(r.quantity),0) FROM RedemptionReservation r WHERE r.user.id = :userId AND r.product.id = :productId AND r.status IN :statuses AND r.createdAt >= :since")
    long sumQuantityByUserAndProductSince(@Param("userId") Long userId,
                                          @Param("productId") Long productId,
                                          @Param("statuses") Collection<RedemptionReservation.Status> statuses,
                                          @Param("since") OffsetDateTime since);

    @Query("SELECT COALESCE(SUM(r.quantity),0) FROM RedemptionReservation r WHERE r.family.id = :familyId AND r.product.id = :productId AND r.status IN :statuses AND r.createdAt >= :since")
    long sumQuantityByFamilyAndProductSince(@Param("familyId") Long familyId,
                                            @Param("productId") Long productId,
                                            @Param("statuses") Collection<RedemptionReservation.Status> statuses,
                                            @Param("since") OffsetDateTime since);

    /** 各商品排队总量（采购建议） */
    @Query("SELECT r.product.id, COALESCE(SUM(r.quantity),0), COUNT(r) FROM RedemptionReservation r WHERE r.status = 'WAITING' GROUP BY r.product.id")
    List<Object[]> waitingStatsByProduct();

    // ---------- 条件状态流转（原子，防并发重复处理） ----------

    /** 领取：仅 READY 且未过领取期才允许，返回 0 表示已被处理或已过期。 */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RedemptionReservation r SET r.status = 'FULFILLED', r.fulfilledAt = :now " +
            "WHERE r.id = :id AND r.status = 'READY' AND (r.expireAt IS NULL OR r.expireAt > :now)")
    int fulfillIfReady(@Param("id") Long id, @Param("now") OffsetDateTime now);

    /** 到期回滚：仅 READY 才关闭，返回 0 表示已被领取/取消（防重复退款）。 */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RedemptionReservation r SET r.status = 'EXPIRED', r.cancelledAt = :now " +
            "WHERE r.id = :id AND r.status = 'READY'")
    int expireIfReady(@Param("id") Long id, @Param("now") OffsetDateTime now);

    /** 取消（排队中）：仅 WAITING 可流转，返回 0 表示状态已变化。 */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RedemptionReservation r SET r.status = 'CANCELLED', r.cancelledAt = :now " +
            "WHERE r.id = :id AND r.status = 'WAITING'")
    int cancelIfWaiting(@Param("id") Long id, @Param("now") OffsetDateTime now);

    /** 取消（待领取）：仅 READY 可流转，返回 0 表示状态已变化。 */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RedemptionReservation r SET r.status = 'CANCELLED', r.cancelledAt = :now " +
            "WHERE r.id = :id AND r.status = 'READY'")
    int cancelIfReady(@Param("id") Long id, @Param("now") OffsetDateTime now);

    /** 替代领取登记：仅 WAITING 且未领过替代，返回 0 表示重复或状态不符。 */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RedemptionReservation r SET r.altProduct.id = :altProductId, r.altOrderId = :altOrderId, r.altFulfilledAt = :now " +
            "WHERE r.id = :id AND r.status = 'WAITING' AND r.altFulfilledAt IS NULL")
    int altPickupIfWaiting(@Param("id") Long id, @Param("altProductId") Long altProductId,
                           @Param("altOrderId") Long altOrderId, @Param("now") OffsetDateTime now);
}
