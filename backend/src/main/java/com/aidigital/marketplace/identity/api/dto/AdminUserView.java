package com.aidigital.marketplace.identity.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserView(
        Long id, String username, String nickname, String status, List<String> roles, LocalDateTime createdAt) {}
