package com.community.waste.repo;

import com.community.waste.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingRepo extends JpaRepository<Building, Long> {
    Optional<Building> findByCode(String code);
}
