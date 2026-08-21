package com.aidigital.marketplace.cms.api.dto;

import java.time.LocalDateTime;

import com.aidigital.marketplace.cms.infrastructure.entity.AnnouncementEntity;

public record AnnouncementView(
        Long id, String title, String body, String status, LocalDateTime publishedAt, LocalDateTime createdAt) {

    public static AnnouncementView from(AnnouncementEntity entity) {
        return new AnnouncementView(
                entity.getId(),
                entity.getTitle(),
                entity.getBody(),
                entity.getStatus(),
                entity.getPublishedAt(),
                entity.getCreatedAt());
    }
}
