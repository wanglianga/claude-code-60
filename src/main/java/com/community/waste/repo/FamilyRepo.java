package com.community.waste.repo;

import com.community.waste.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FamilyRepo extends JpaRepository<Family, Long> {
    Optional<Family> findByName(String name);
}
