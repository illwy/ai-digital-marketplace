package com.aidigital.marketplace.delivery.api.dto;

import java.time.LocalDateTime;

public record DeliveryView(
        Long id,
        Long orderId,
        Long inventoryId,
        String status,
        String content,
        String remark,
        LocalDateTime deliveredAt) {}
