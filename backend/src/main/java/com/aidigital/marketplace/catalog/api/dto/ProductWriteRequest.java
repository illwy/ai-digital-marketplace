package com.aidigital.marketplace.catalog.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductWriteRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 20000) String description,
        @Size(max = 512) String coverUrl,
        @NotNull @Min(0) Integer priceFen,
        @NotBlank @Pattern(regexp = "ACCOUNT|LICENSE|TOKEN|TEXT|NODE_SUBSCRIPTION") String deliveryType,
        @NotBlank @Pattern(regexp = "DRAFT|ON_SALE|OFF_SALE") String status) {}
