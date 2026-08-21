package com.aidigital.marketplace.catalog.api.dto;

import com.aidigital.marketplace.catalog.infrastructure.entity.ProductEntity;

public record ProductView(
        Long id,
        Long categoryId,
        String name,
        String description,
        String coverUrl,
        Integer priceFen,
        String deliveryType,
        String status,
        long availableCount) {

    public static ProductView from(ProductEntity entity, long availableCount) {
        return new ProductView(
                entity.getId(),
                entity.getCategoryId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCoverUrl(),
                entity.getPriceFen(),
                entity.getDeliveryType(),
                entity.getStatus(),
                availableCount);
    }
}
