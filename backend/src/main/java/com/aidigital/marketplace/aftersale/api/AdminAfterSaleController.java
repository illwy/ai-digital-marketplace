package com.aidigital.marketplace.aftersale.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.aftersale.api.dto.AfterSaleHandleRequest;
import com.aidigital.marketplace.aftersale.api.dto.AfterSaleView;
import com.aidigital.marketplace.aftersale.application.AfterSaleService;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.shared.web.ListResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/after-sale-tickets")
public class AdminAfterSaleController {

    private final AfterSaleService afterSaleService;

    public AdminAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @GetMapping
    public ListResponse<AfterSaleView> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return afterSaleService.listAdmin(status, page, pageSize);
    }

    @PostMapping("/{id}/handle")
    public DataResponse<AfterSaleView> handle(
            @PathVariable Long id, @Valid @RequestBody AfterSaleHandleRequest request) {
        return new DataResponse<>(afterSaleService.handle(id, request.status(), request.adminReply()));
    }
}
