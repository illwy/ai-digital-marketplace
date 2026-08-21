package com.aidigital.marketplace.cms.application;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.cms.api.dto.AnnouncementView;
import com.aidigital.marketplace.cms.api.dto.AnnouncementWriteRequest;
import com.aidigital.marketplace.cms.infrastructure.entity.AnnouncementEntity;
import com.aidigital.marketplace.cms.infrastructure.mapper.AnnouncementMapper;
import com.aidigital.marketplace.shared.web.ApiException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    public AnnouncementService(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    public List<AnnouncementView> listEnabled() {
        return announcementMapper
                .selectList(new LambdaQueryWrapper<AnnouncementEntity>()
                        .eq(AnnouncementEntity::getStatus, "ENABLED")
                        .orderByDesc(AnnouncementEntity::getPublishedAt)
                        .orderByDesc(AnnouncementEntity::getId))
                .stream()
                .map(AnnouncementView::from)
                .toList();
    }

    public List<AnnouncementView> listAll() {
        return announcementMapper
                .selectList(new LambdaQueryWrapper<AnnouncementEntity>().orderByDesc(AnnouncementEntity::getId))
                .stream()
                .map(AnnouncementView::from)
                .toList();
    }

    @Transactional
    public AnnouncementView create(AnnouncementWriteRequest request) {
        AnnouncementEntity entity = new AnnouncementEntity();
        apply(entity, request);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        announcementMapper.insert(entity);
        return AnnouncementView.from(entity);
    }

    @Transactional
    public AnnouncementView update(Long id, AnnouncementWriteRequest request) {
        AnnouncementEntity entity = require(id);
        apply(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(entity);
        return AnnouncementView.from(entity);
    }

    private void apply(AnnouncementEntity entity, AnnouncementWriteRequest request) {
        entity.setTitle(request.title());
        entity.setBody(request.body());
        entity.setStatus(request.status());
        if ("ENABLED".equals(request.status()) && entity.getPublishedAt() == null) {
            entity.setPublishedAt(LocalDateTime.now());
        }
    }

    private AnnouncementEntity require(Long id) {
        AnnouncementEntity entity = announcementMapper.selectById(id);
        if (entity == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ANNOUNCEMENT_NOT_FOUND", "公告不存在");
        }
        return entity;
    }
}
