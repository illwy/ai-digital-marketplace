package com.aidigital.marketplace.order.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.order.api.dto.OrderView;
import com.aidigital.marketplace.order.application.OrderService;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.shared.web.ListResponse;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ListResponse<OrderView> list(
            @RequestParam(required = false) String payStatus,
            @RequestParam(required = false) String deliveryStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return orderService.listAdmin(payStatus, deliveryStatus, page, pageSize);
    }

    @GetMapping("/{id}")
    public DataResponse<OrderView> detail(@PathVariable Long id) {
        return new DataResponse<>(orderService.getAdmin(id));
    }

    @PostMapping("/{id}/cancel")
    public DataResponse<OrderView> cancel(@PathVariable Long id) {
        return new DataResponse<>(orderService.cancel(id, null, true, "CANCELLED"));
    }
}
