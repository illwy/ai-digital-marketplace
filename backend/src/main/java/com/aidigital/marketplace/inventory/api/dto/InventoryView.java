package com.aidigital.marketplace.inventory.api.dto;

import java.time.LocalDateTime;

import com.aidigital.marketplace.inventory.infrastructure.entity.InventoryEntity;
import com.aidigital.marketplace.shared.web.ContentMasker;

public record InventoryView(
        Long id,
        Long productId,
        String maskedContent,
        String status,
        Long orderId,
        String remark,
        LocalDateTime createdAt) {

    public static InventoryView from(InventoryEntity entity) {
        return new InventoryView(
                entity.getId(),
                entity.getProductId(),
                ContentMasker.mask(entity.getContent()),
                entity.getStatus(),
                entity.getOrderId(),
                entity.getRemark(),
                entity.getCreatedAt());
    }
}
