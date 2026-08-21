package com.aidigital.marketplace.identity.api;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.identity.security.AuthUser;
import com.aidigital.marketplace.shared.web.DataResponse;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminPingController {

    @GetMapping("/ping")
    public DataResponse<Map<String, String>> ping(@AuthenticationPrincipal AuthUser user) {
        return new DataResponse<>(Map.of("status", "ok", "username", user.getUsername()));
    }
}
