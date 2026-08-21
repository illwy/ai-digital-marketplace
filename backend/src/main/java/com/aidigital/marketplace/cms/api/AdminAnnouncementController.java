package com.aidigital.marketplace.cms.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.cms.api.dto.AnnouncementView;
import com.aidigital.marketplace.cms.api.dto.AnnouncementWriteRequest;
import com.aidigital.marketplace.cms.application.AnnouncementService;
import com.aidigital.marketplace.shared.web.DataResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/announcements")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    public AdminAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public DataResponse<List<AnnouncementView>> list() {
        return new DataResponse<>(announcementService.listAll());
    }

    @PostMapping
    public DataResponse<AnnouncementView> create(@Valid @RequestBody AnnouncementWriteRequest request) {
        return new DataResponse<>(announcementService.create(request));
    }

    @PutMapping("/{id}")
    public DataResponse<AnnouncementView> update(
            @PathVariable Long id, @Valid @RequestBody AnnouncementWriteRequest request) {
        return new DataResponse<>(announcementService.update(id, request));
    }
}
