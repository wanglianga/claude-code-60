package com.community.waste.repo;

import com.community.waste.model.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepo extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    List<AppUser> findByRole(AppUser.Role role);

    List<AppUser> findByFamilyId(Long familyId);

    List<AppUser> findByRoleAndBuildingId(AppUser.Role role, Long buildingId);

    /** 行锁读取用户（积分余额扣减串行化）。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM AppUser u WHERE u.id = :id")
    Optional<AppUser> findByIdForUpdate(@Param("id") Long id);

    /** 行锁读取整个家庭（按 id 排序加锁，避免死锁），用于家庭共享代付。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM AppUser u WHERE u.family.id = :familyId ORDER BY u.id")
    List<AppUser> findByFamilyIdForUpdate(@Param("familyId") Long familyId);
}
