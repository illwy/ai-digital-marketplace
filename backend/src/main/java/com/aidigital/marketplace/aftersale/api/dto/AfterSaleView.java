package com.aidigital.marketplace.aftersale.api.dto;

import java.time.LocalDateTime;

import com.aidigital.marketplace.aftersale.infrastructure.entity.AfterSaleEntity;

public record AfterSaleView(
        Long id,
        Long orderId,
        Long userId,
        String reason,
        String status,
        String adminReply,
        LocalDateTime createdAt) {

    public static AfterSaleView from(AfterSaleEntity entity) {
        return new AfterSaleView(
                entity.getId(),
                entity.getOrderId(),
                entity.getUserId(),
                entity.getReason(),
                entity.getStatus(),
                entity.getAdminReply(),
                entity.getCreatedAt());
    }
}
