package com.aidigital.marketplace.identity.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.identity.api.dto.LoginRequest;
import com.aidigital.marketplace.identity.api.dto.LogoutRequest;
import com.aidigital.marketplace.identity.api.dto.RefreshRequest;
import com.aidigital.marketplace.identity.api.dto.RegisterRequest;
import com.aidigital.marketplace.identity.api.dto.TokenResponse;
import com.aidigital.marketplace.identity.application.AuthService;
import com.aidigital.marketplace.shared.web.DataResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public DataResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new DataResponse<>(TokenResponse.from(
                authService.register(request.username(), request.password(), request.nickname())));
    }

    @PostMapping("/login")
    public DataResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return new DataResponse<>(TokenResponse.from(authService.login(request.username(), request.password())));
    }

    @PostMapping("/refresh")
    public DataResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return new DataResponse<>(TokenResponse.from(authService.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    public DataResponse<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
        return new DataResponse<>(null);
    }
}
