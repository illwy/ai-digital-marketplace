package com.aidigital.marketplace.wallet.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalletCreditRequest(@NotNull @Min(1) @Max(1_000_000) Integer amountFen) {}
