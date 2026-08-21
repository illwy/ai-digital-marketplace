package com.aidigital.marketplace.aftersale.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.aftersale.api.dto.AfterSaleCreateRequest;
import com.aidigital.marketplace.aftersale.api.dto.AfterSaleView;
import com.aidigital.marketplace.aftersale.application.AfterSaleService;
import com.aidigital.marketplace.identity.security.AuthUser;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.shared.web.ListResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/after-sale-tickets")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    public AfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @PostMapping
    public DataResponse<AfterSaleView> create(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody AfterSaleCreateRequest request) {
        return new DataResponse<>(afterSaleService.create(user.getId(), request.orderId(), request.reason()));
    }

    @GetMapping
    public ListResponse<AfterSaleView> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return afterSaleService.listMine(user.getId(), page, pageSize);
    }
}
