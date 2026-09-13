package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.model.AppUser;
import com.community.waste.service.UserService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    /** 按角色列出用户（如督导员代录时选择居民）。 */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR','PROPERTY','GOVERNANCE','ADMIN')")
    public List<AppUser> list(@RequestParam(defaultValue = "RESIDENT") String role) {
        return userService.listByRole(AppUser.Role.valueOf(role));
    }

    /** 我的家庭成员（积分共享池）。 */
    @GetMapping("/family")
    public List<AppUser> family() {
        return userService.familyMembers(currentUser.require());
    }

    public record AdjustRequest(@NotNull Integer delta, String note) {
    }

    /** 管理员人工调整积分。 */
    @PostMapping("/{id}/adjust-points")
    @PreAuthorize("hasRole('ADMIN')")
    public AppUser adjust(@PathVariable Long id, @RequestBody AdjustRequest req) {
        return userService.adjustPoints(id, req.delta(), req.note());
    }
}
