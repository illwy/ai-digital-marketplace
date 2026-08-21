package com.aidigital.marketplace.inventory.api.dto;

public record InventoryStatsView(long available, long locked, long sold, long invalid) {}
