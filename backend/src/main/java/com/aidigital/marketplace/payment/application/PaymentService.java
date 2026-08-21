package com.aidigital.marketplace.payment.application;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.delivery.application.DeliveryService;
import com.aidigital.marketplace.inventory.application.InventoryService;
import com.aidigital.marketplace.order.application.OrderService;
import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.aidigital.marketplace.payment.api.dto.PaymentView;
import com.aidigital.marketplace.payment.infrastructure.entity.PaymentEntity;
import com.aidigital.marketplace.payment.infrastructure.mapper.PaymentMapper;
import com.aidigital.marketplace.shared.application.BizNos;
import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.wallet.application.WalletService;

@Service
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderService orderService;
    private final WalletService walletService;
    private final InventoryService inventoryService;
    private final DeliveryService deliveryService;

    public PaymentService(
            PaymentMapper paymentMapper,
            OrderService orderService,
            WalletService walletService,
            InventoryService inventoryService,
            DeliveryService deliveryService) {
        this.paymentMapper = paymentMapper;
        this.orderService = orderService;
        this.walletService = walletService;
        this.inventoryService = inventoryService;
        this.deliveryService = deliveryService;
    }

    @Transactional
    public PaymentView pay(Long userId, Long orderId, String channel) {
        OrderEntity order = orderService.require(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在");
        }
        if (!"PENDING".equals(order.getPayStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_NOT_PENDING", "订单不可支付");
        }
        if (order.getExpireAt() != null && order.getExpireAt().isBefore(LocalDateTime.now())) {
            orderService.cancel(order.getId(), userId, true, "EXPIRED");
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_EXPIRED", "订单已过期");
        }
        PaymentEntity payment = new PaymentEntity();
        payment.setPaymentNo(BizNos.next("PAY"));
        payment.setOrderId(order.getId());
        payment.setChannel(channel);
        payment.setAmountFen(order.getAmountFen());
        payment.setStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        if ("WALLET".equals(channel)) {
            walletService.debit(userId, order.getAmountFen(), payment.getPaymentNo(), "订单支付");
        } else if (!"SANDBOX".equals(channel)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CHANNEL_UNSUPPORTED", "暂不支持该支付渠道");
        }
        fulfill(payment, order);
        return toView(payment);
    }

    private void fulfill(PaymentEntity payment, OrderEntity order) {
        payment.setStatus("SUCCESS");
        payment.setChannelTradeNo(payment.getPaymentNo());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        boolean sold = order.getInventoryId() != null
                && inventoryService.markSold(order.getInventoryId(), order.getId());
        String deliveryStatus = sold ? "DELIVERED" : "FAILED";
        deliveryService.create(
                order.getId(),
                order.getUserId(),
                order.getInventoryId(),
                deliveryStatus,
                sold ? null : "库存状态异常，需人工处理");
        orderService.markPaidAndDeliver(order, deliveryStatus);
    }

    private PaymentView toView(PaymentEntity payment) {
        return new PaymentView(
                payment.getId(),
                payment.getPaymentNo(),
                payment.getOrderId(),
                payment.getChannel(),
                payment.getAmountFen(),
                payment.getStatus());
    }
}
