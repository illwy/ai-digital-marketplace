package com.aidigital.marketplace.payment.api.dto;

public record PayChannelView(boolean alipayEnabled, boolean walletEnabled, boolean sandboxEnabled) {}
