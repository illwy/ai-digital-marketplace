package com.aidigital.marketplace.order.api.dto;

import java.time.LocalDateTime;

public record OrderView(
        Long id,
        String orderNo,
        Long userId,
        Integer amountFen,
        String payStatus,
        String deliveryStatus,
        String aftersaleStatus,
        Long inventoryId,
        Long productId,
        String productName,
        LocalDateTime expireAt,
        LocalDateTime paidAt,
        LocalDateTime createdAt) {}
