package com.aidigital.marketplace.payment.api.dto;

public record PaymentView(
        Long id, String paymentNo, Long orderId, String channel, Integer amountFen, String status) {}
