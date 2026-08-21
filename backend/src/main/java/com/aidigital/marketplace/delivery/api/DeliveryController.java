package com.aidigital.marketplace.delivery.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.delivery.api.dto.DeliveryView;
import com.aidigital.marketplace.delivery.application.DeliveryService;
import com.aidigital.marketplace.identity.security.AuthUser;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.shared.web.ListResponse;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public ListResponse<DeliveryView> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return deliveryService.listMine(user.getId(), page, pageSize);
    }

    @GetMapping("/{id}")
    public DataResponse<DeliveryView> detail(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return new DataResponse<>(deliveryService.getMine(user.getId(), id));
    }
}
