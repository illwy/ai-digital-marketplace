package com.aidigital.marketplace.inventory.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryImportRequest(
        @NotNull Long productId,
        @NotEmpty List<@Size(min = 1, max = 2000) String> contents,
        @Size(max = 255) String remark) {}
