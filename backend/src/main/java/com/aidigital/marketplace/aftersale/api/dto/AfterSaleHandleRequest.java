package com.aidigital.marketplace.aftersale.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AfterSaleHandleRequest(
        @NotBlank @Pattern(regexp = "PROCESSING|CLOSED") String status, @Size(max = 512) String adminReply) {}
