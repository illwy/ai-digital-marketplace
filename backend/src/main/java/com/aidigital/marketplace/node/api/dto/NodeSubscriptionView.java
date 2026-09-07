package com.aidigital.marketplace.node.api.dto;

import java.time.LocalDateTime;

import com.aidigital.marketplace.node.infrastructure.entity.NodeSubscriptionEntity;

public record NodeSubscriptionView(
        Long id,
        Long productId,
        String clientName,
        Long trafficBytes,
        Integer deviceLimit,
        LocalDateTime expiresAt,
        String subscriptionUrl,
        String clashConfig,
        String status) {

    public static NodeSubscriptionView from(NodeSubscriptionEntity entity) {
        return new NodeSubscriptionView(
                entity.getId(), entity.getProductId(), entity.getClientName(), entity.getTrafficBytes(),
                entity.getDeviceLimit(), entity.getExpiresAt(), entity.getSubscriptionUrl(),
                entity.getClashConfig(), entity.getStatus());
    }
}
