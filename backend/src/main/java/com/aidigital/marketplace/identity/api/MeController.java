package com.aidigital.marketplace.identity.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.identity.api.dto.UserView;
import com.aidigital.marketplace.identity.security.AuthUser;
import com.aidigital.marketplace.shared.web.DataResponse;

@RestController
@RequestMapping("/api/v1")
public class MeController {

    @GetMapping("/me")
    public DataResponse<UserView> me(@AuthenticationPrincipal AuthUser user) {
        return new DataResponse<>(UserView.from(user));
    }
}
