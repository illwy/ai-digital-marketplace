package com.aidigital.marketplace.node.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NodePlanWriteRequest(
        @NotNull Long productId,
        @NotBlank String apiBaseUrl,
        String webPath,
        @NotBlank String inboundIdsJson,
        @NotNull @Min(0) Long trafficBytes,
        @NotNull @Min(1) Integer durationDays,
        @NotNull @Min(0) Integer deviceLimit,
        @NotNull Boolean enabled) {}
