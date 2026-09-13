package com.community.waste.service;

import com.community.waste.config.JwtService;
import com.community.waste.exception.ApiException;
import com.community.waste.model.AppUser;
import com.community.waste.repo.AppUserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private final AppUserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepo userRepo, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Map<String, Object> login(String username, String password) {
        AppUser user = userRepo.findByUsername(username)
                .orElseThrow(() -> ApiException.forbidden("用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw ApiException.forbidden("用户名或密码错误");
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("token", jwtService.issue(user));
        resp.put("user", user);
        return resp;
    }
}
