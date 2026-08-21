package com.aidigital.marketplace.cms.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.cms.api.dto.AnnouncementView;
import com.aidigital.marketplace.cms.application.AnnouncementService;
import com.aidigital.marketplace.shared.web.DataResponse;

@RestController
@RequestMapping("/api/v1/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public DataResponse<List<AnnouncementView>> list() {
        return new DataResponse<>(announcementService.listEnabled());
    }
}
