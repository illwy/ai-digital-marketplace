package com.aidigital.marketplace.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserStatusRequest(@NotBlank @Pattern(regexp = "ENABLED|DISABLED") String status) {}
