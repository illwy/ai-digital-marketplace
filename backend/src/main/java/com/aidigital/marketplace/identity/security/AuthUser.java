package com.aidigital.marketplace.identity.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String nickname;
    private final boolean enabled;
    private final List<String> roles;

    public AuthUser(Long id, String username, String passwordHash, String nickname, boolean enabled, List<String> roles) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.enabled = enabled;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
