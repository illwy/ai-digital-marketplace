package com.aidigital.marketplace.shared.web;

public record Pagination(int page, int pageSize, long totalItems, int totalPages) {

    public static Pagination of(int page, int pageSize, long totalItems) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / safeSize);
        return new Pagination(safePage, safeSize, totalItems, totalPages);
    }
}
