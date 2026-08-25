package com.aidigital.marketplace.order.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.identity.security.AuthUser;
import com.aidigital.marketplace.order.api.dto.CreateOrderRequest;
import com.aidigital.marketplace.order.api.dto.OrderView;
import com.aidigital.marketplace.order.application.OrderService;
import com.aidigital.marketplace.payment.api.dto.CreatePaymentRequest;
import com.aidigital.marketplace.payment.api.dto.PaymentView;
import com.aidigital.marketplace.payment.application.PaymentService;
import com.aidigital.marketplace.shared.application.IdempotencyService;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.shared.web.ListResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public OrderController(
            OrderService orderService, PaymentService paymentService, IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public DataResponse<OrderView> create(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        OrderView view = idempotencyService.run(
                idempotencyKey, request, OrderView.class, () -> orderService.create(user.getId(), request.productId()));
        return new DataResponse<>(view);
    }

    @GetMapping
    public ListResponse<OrderView> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return orderService.listMine(user.getId(), page, pageSize);
    }

    @GetMapping("/{id}")
    public DataResponse<OrderView> detail(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return new DataResponse<>(orderService.getMine(user.getId(), id));
    }

    @PostMapping("/{id}/cancel")
    public DataResponse<OrderView> cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return new DataResponse<>(orderService.cancel(id, user.getId(), false, "CANCELLED"));
    }

    @PostMapping("/{id}/payments")
    public DataResponse<PaymentView> pay(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long id,
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PaymentView view = idempotencyService.run(
                idempotencyKey,
                request,
                PaymentView.class,
                () -> paymentService.pay(user.getId(), id, request.channel()));
        return new DataResponse<>(view);
    }

    @PostMapping("/{id}/payments/sync")
    public DataResponse<PaymentView> syncAlipay(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return new DataResponse<>(paymentService.syncAlipay(user.getId(), id));
    }
}
