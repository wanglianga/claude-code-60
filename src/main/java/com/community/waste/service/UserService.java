package com.community.waste.service;

import com.community.waste.exception.ApiException;
import com.community.waste.model.AppUser;
import com.community.waste.model.PointsTransaction;
import com.community.waste.repo.AppUserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final AppUserRepo userRepo;
    private final PointsService pointsService;

    public UserService(AppUserRepo userRepo, PointsService pointsService) {
        this.userRepo = userRepo;
        this.pointsService = pointsService;
    }

    @Transactional(readOnly = true)
    public List<AppUser> listByRole(AppUser.Role role) {
        return userRepo.findByRole(role);
    }

    @Transactional(readOnly = true)
    public List<AppUser> familyMembers(AppUser user) {
        if (user.getFamily() == null) {
            return List.of(user);
        }
        return userRepo.findByFamilyId(user.getFamily().getId());
    }

    /** 管理员人工调整积分。 */
    @Transactional
    public AppUser adjustPoints(Long userId, int delta, String note) {
        AppUser user = userRepo.findById(userId).orElseThrow(() -> ApiException.notFound("用户不存在"));
        pointsService.apply(user, delta, PointsTransaction.TxType.ADJUST, "MANUAL", null,
                note == null ? "人工调整" : note);
        return userRepo.findById(userId).orElseThrow();
    }
}
