package com.aidigital.marketplace.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryWriteRequest(
        @NotBlank @Size(max = 64) String name,
        @NotNull Integer sortOrder,
        @NotBlank @Pattern(regexp = "ENABLED|DISABLED") String status) {}
