package com.aidigital.marketplace.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreatePaymentRequest(@NotBlank @Pattern(regexp = "WALLET|SANDBOX") String channel) {}
