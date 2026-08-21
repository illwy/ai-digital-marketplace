package com.aidigital.marketplace.catalog.api.dto;

import com.aidigital.marketplace.catalog.infrastructure.entity.CategoryEntity;

public record CategoryView(Long id, String name, Integer sortOrder, String status) {

    public static CategoryView from(CategoryEntity entity) {
        return new CategoryView(entity.getId(), entity.getName(), entity.getSortOrder(), entity.getStatus());
    }
}
