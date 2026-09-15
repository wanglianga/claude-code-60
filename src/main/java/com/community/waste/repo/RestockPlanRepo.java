package com.community.waste.repo;

import com.community.waste.model.RestockPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RestockPlanRepo extends JpaRepository<RestockPlan, Long> {

    List<RestockPlan> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<RestockPlan> findByStatusOrderByExpectedAtAsc(RestockPlan.Status status);

    /** 某商品最近的预计到货时间（用于排队页提示） */
    @Query("SELECT p FROM RestockPlan p WHERE p.product.id = :productId AND p.status = 'PLANNED' ORDER BY p.expectedAt ASC LIMIT 1")
    Optional<RestockPlan> findNextPlanned(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(p.quantity),0) FROM RestockPlan p WHERE p.product.id = :productId AND p.status = 'PLANNED'")
    long sumPlannedQuantity(@Param("productId") Long productId);

    /** 条件流转：仅 PLANNED 可到货，返回 0 表示已被处理（防重复入库）。 */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RestockPlan p SET p.status = 'ARRIVED', p.arrivedAt = :now WHERE p.id = :id AND p.status = 'PLANNED'")
    int arriveIfPlanned(@Param("id") Long id, @Param("now") OffsetDateTime now);
}
