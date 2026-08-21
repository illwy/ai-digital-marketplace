package com.aidigital.marketplace.order.api.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(@NotNull Long productId) {}
