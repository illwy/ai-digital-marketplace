package com.aidigital.marketplace.node.api.dto;

import java.time.LocalDateTime;
import com.aidigital.marketplace.node.infrastructure.entity.NodeSubscriptionEntity;

public record NodeSubscriptionAdminView(
        Long id, Long userId, Long productId, String username, String clientName,
        LocalDateTime expiresAt, String subscriptionUrl, String status) {
    public static NodeSubscriptionAdminView from(NodeSubscriptionEntity entity) {
        return new NodeSubscriptionAdminView(entity.getId(), entity.getUserId(), entity.getProductId(),
                "用户 #" + entity.getUserId(), entity.getClientName(), entity.getExpiresAt(),
                entity.getSubscriptionUrl(), entity.getStatus());
    }
}
