package com.aidigital.marketplace.node.api.dto;

import java.time.LocalDateTime;

import com.aidigital.marketplace.node.infrastructure.entity.NodeProvisionJobEntity;

public record NodeProvisionJobView(
        Long id,
        Long orderId,
        Long userId,
        Long productId,
        String action,
        String status,
        Integer attempts,
        LocalDateTime nextRunAt,
        String lastError,
        LocalDateTime updatedAt) {

    public static NodeProvisionJobView from(NodeProvisionJobEntity entity) {
        return new NodeProvisionJobView(
                entity.getId(), entity.getOrderId(), entity.getUserId(), entity.getProductId(),
                entity.getAction(), entity.getStatus(), entity.getAttempts(), entity.getNextRunAt(),
                entity.getLastError(), entity.getUpdatedAt());
    }
}
