package com.community.waste.repo;

import com.community.waste.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRepo extends JpaRepository<Policy, Long> {
    List<Policy> findByActiveTrue();
}
