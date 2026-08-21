package com.aidigital.marketplace.identity.api.dto;

import com.aidigital.marketplace.identity.application.AuthService;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn, UserView user) {

    public static TokenResponse from(AuthService.TokenBundle bundle) {
        return new TokenResponse(
                bundle.accessToken(), bundle.refreshToken(), bundle.expiresIn(), UserView.from(bundle.user()));
    }
}
