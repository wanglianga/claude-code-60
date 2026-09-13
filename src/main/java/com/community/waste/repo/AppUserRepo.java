package com.community.waste.repo;

import com.community.waste.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepo extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    List<AppUser> findByRole(AppUser.Role role);

    List<AppUser> findByFamilyId(Long familyId);

    List<AppUser> findByRoleAndBuildingId(AppUser.Role role, Long buildingId);
}
