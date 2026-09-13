package com.community.waste.config;

import com.community.waste.exception.ApiException;
import com.community.waste.model.AppUser;
import com.community.waste.repo.AppUserRepo;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final AppUserRepo userRepo;

    public CurrentUser(AppUserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public AppUser require() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw ApiException.forbidden("未登录");
        }
        return userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> ApiException.forbidden("用户不存在"));
    }
}
