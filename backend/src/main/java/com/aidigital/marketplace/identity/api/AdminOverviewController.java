package com.aidigital.marketplace.identity.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.identity.api.dto.AdminOverviewView;
import com.aidigital.marketplace.identity.application.AdminOverviewService;
import com.aidigital.marketplace.shared.web.DataResponse;

@RestController
@RequestMapping("/api/v1/admin/overview")
public class AdminOverviewController {

    private final AdminOverviewService adminOverviewService;

    public AdminOverviewController(AdminOverviewService adminOverviewService) {
        this.adminOverviewService = adminOverviewService;
    }

    @GetMapping
    public DataResponse<AdminOverviewView> overview() {
        return new DataResponse<>(adminOverviewService.load());
    }
}
