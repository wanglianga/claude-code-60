package com.community.waste.repo;

import com.community.waste.model.RestockPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
