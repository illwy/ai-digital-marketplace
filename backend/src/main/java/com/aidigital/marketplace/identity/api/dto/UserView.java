package com.aidigital.marketplace.identity.api.dto;

import java.util.List;

import com.aidigital.marketplace.identity.security.AuthUser;

public record UserView(Long id, String username, String nickname, List<String> roles) {

    public static UserView from(AuthUser user) {
        return new UserView(user.getId(), user.getUsername(), user.getNickname(), user.getRoles());
    }
}
