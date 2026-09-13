package com.community.waste.repo;

import com.community.waste.model.Violation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface ViolationRepo extends JpaRepository<Violation, Long> {

    List<Violation> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndLevelInAndCreatedAtAfter(Long userId, Collection<Violation.Level> levels, OffsetDateTime after);

    long countByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);

    long countByUserIdAndCreatedAtAfter(Long userId, OffsetDateTime after);
}
