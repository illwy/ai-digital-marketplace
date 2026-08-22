package com.aidigital.marketplace.delivery.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.delivery.api.dto.DeliveryView;
import com.aidigital.marketplace.delivery.application.DeliveryService;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.shared.web.ListResponse;

@RestController
@RequestMapping("/api/v1/admin/deliveries")
public class AdminDeliveryController {

    public record RemarkRequest(String remark) {}

    private final DeliveryService deliveryService;

    public AdminDeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public ListResponse<DeliveryView> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return deliveryService.listAdmin(orderId, page, pageSize);
    }

    @PostMapping("/{id}/remarks")
    public DataResponse<DeliveryView> remark(@PathVariable Long id, @RequestBody RemarkRequest request) {
        return new DataResponse<>(deliveryService.remarkFailed(id, request.remark()));
    }
}
