package com.aidigital.marketplace.payment.api.dto;

public record PaymentView(
        Long id,
        String paymentNo,
        Long orderId,
        String channel,
        Integer amountFen,
        String status,
        String paymentHtml) {

    public static PaymentView of(
            Long id, String paymentNo, Long orderId, String channel, Integer amountFen, String status) {
        return new PaymentView(id, paymentNo, orderId, channel, amountFen, status, null);
    }
}
