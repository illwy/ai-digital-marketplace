package com.aidigital.marketplace.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 32) @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名仅允许字母数字下划线")
                String username,
        @NotBlank @Size(min = 8, max = 64) String password,
        @Size(max = 64) String nickname) {
}
