package com.community.waste.repo;

import com.community.waste.model.BucketPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BucketPointRepo extends JpaRepository<BucketPoint, Long> {
    Optional<BucketPoint> findByCode(String code);

    List<BucketPoint> findByStatus(BucketPoint.Status status);

    List<BucketPoint> findByBuildingId(Long buildingId);
}
