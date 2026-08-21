package com.aidigital.marketplace.aftersale.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AfterSaleCreateRequest(@NotNull Long orderId, @NotBlank @Size(max = 512) String reason) {}
