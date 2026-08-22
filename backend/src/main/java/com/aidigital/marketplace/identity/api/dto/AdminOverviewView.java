package com.aidigital.marketplace.identity.api.dto;

public record AdminOverviewView(
        long userCount,
        long productOnSaleCount,
        long availableInventoryCount,
        long pendingOrderCount,
        long paidOrderCount,
        long openAfterSaleCount,
        long enabledAnnouncementCount) {}
