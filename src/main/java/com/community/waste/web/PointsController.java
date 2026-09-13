package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.model.AppUser;
import com.community.waste.model.PointsTransaction;
import com.community.waste.model.Violation;
import com.community.waste.repo.PointsTransactionRepo;
import com.community.waste.repo.ViolationRepo;
import com.community.waste.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/points")
public class PointsController {

    private final CurrentUser currentUser;
    private final PointsTransactionRepo txRepo;
    private final ViolationRepo violationRepo;
    private final UserService userService;

    public PointsController(CurrentUser currentUser, PointsTransactionRepo txRepo,
                            ViolationRepo violationRepo, UserService userService) {
        this.currentUser = currentUser;
        this.txRepo = txRepo;
        this.violationRepo = violationRepo;
        this.userService = userService;
    }

    /** 我的积分：余额 + 家庭成员共享池。 */
    @GetMapping("/me")
    public Map<String, Object> me() {
        AppUser user = currentUser.require();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("balance", user.getPointsBalance());
        List<AppUser> members = userService.familyMembers(user);
        m.put("familyMembers", members.stream().map(u -> {
            Map<String, Object> fm = new LinkedHashMap<String, Object>();
            fm.put("id", u.getId());
            fm.put("name", u.getDisplayName());
            fm.put("balance", u.getPointsBalance());
            return fm;
        }).toList());
        m.put("familyTotal", members.stream().mapToInt(AppUser::getPointsBalance).sum());
        return m;
    }

    @GetMapping("/transactions")
    public List<PointsTransaction> transactions() {
        return txRepo.findTop50ByUserIdOrderByCreatedAtDesc(currentUser.require().getId());
    }

    /** 我的违规与教育提醒。 */
    @GetMapping("/violations")
    public List<Violation> violations() {
        return violationRepo.findByUserIdOrderByCreatedAtDesc(currentUser.require().getId());
    }
}
