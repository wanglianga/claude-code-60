package com.community.waste.repo;

import com.community.waste.model.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppealRepo extends JpaRepository<Appeal, Long> {

    List<Appeal> findByStatusOrderByCreatedAtDesc(Appeal.Status status);

    List<Appeal> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByPointsTransactionIdAndStatus(Long pointsTransactionId, Appeal.Status status);
}
