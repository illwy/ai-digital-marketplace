package com.aidigital.marketplace.payment.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.delivery.application.DeliveryService;
import com.aidigital.marketplace.inventory.application.InventoryService;
import com.aidigital.marketplace.order.application.OrderService;
import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.aidigital.marketplace.node.application.NodeProvisioningService;
import com.aidigital.marketplace.payment.api.dto.PayChannelView;
import com.aidigital.marketplace.payment.api.dto.PaymentView;
import com.aidigital.marketplace.payment.infrastructure.entity.PaymentEntity;
import com.aidigital.marketplace.payment.infrastructure.mapper.PaymentMapper;
import com.aidigital.marketplace.shared.application.BizNos;
import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.wallet.application.WalletService;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentMapper paymentMapper;
    private final OrderService orderService;
    private final WalletService walletService;
    private final InventoryService inventoryService;
    private final DeliveryService deliveryService;
    private final AlipayGateway alipayGateway;
    private final NodeProvisioningService nodeProvisioningService;

    public PaymentService(
            PaymentMapper paymentMapper,
            OrderService orderService,
            WalletService walletService,
            InventoryService inventoryService,
            DeliveryService deliveryService,
            AlipayGateway alipayGateway,
            NodeProvisioningService nodeProvisioningService) {
        this.paymentMapper = paymentMapper;
        this.orderService = orderService;
        this.walletService = walletService;
        this.inventoryService = inventoryService;
        this.deliveryService = deliveryService;
        this.alipayGateway = alipayGateway;
        this.nodeProvisioningService = nodeProvisioningService;
    }

    public PayChannelView channels() {
        return new PayChannelView(alipayGateway.isConfigured(), true, true);
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
            fulfill(payment, order);
            return toView(payment, null);
        }
        if ("SANDBOX".equals(channel)) {
            fulfill(payment, order);
            return toView(payment, null);
        }
        if ("ALIPAY".equals(channel)) {
            var mine = orderService.getMine(userId, orderId);
            String subject = "钥市 " + mine.productName();
            String form = alipayGateway.pagePayForm(order, payment, subject, mine.productId());
            return toView(payment, form);
        }
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CHANNEL_UNSUPPORTED", "暂不支持该支付渠道");
    }

    @Transactional
    public String handleAlipayNotify(Map<String, String> params) {
        if (!alipayGateway.verifyNotify(params)) {
            return "failure";
        }
        String appId = params.get("app_id");
        String outTradeNo = params.get("out_trade_no");
        String totalAmount = params.get("total_amount");
        String tradeStatus = params.get("trade_status");
        String tradeNo = params.get("trade_no");
        String sellerId = params.get("seller_id");
        if (!alipayGateway.matchesAppId(appId) || !alipayGateway.matchesSeller(sellerId)) {
            return "failure";
        }
        PaymentEntity payment = findByPaymentNo(outTradeNo);
        if (payment == null || !"ALIPAY".equals(payment.getChannel())) {
            return "failure";
        }
        if (!AlipayGateway.fenToYuan(payment.getAmountFen()).equals(totalAmount)) {
            return "failure";
        }
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            confirmPaid(payment, tradeNo, params.toString());
        }
        return "success";
    }

    @Transactional
    public PaymentView syncAlipay(Long userId, Long orderId) {
        OrderEntity order = orderService.require(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在");
        }
        PaymentEntity payment = latestAlipay(orderId);
        if (payment == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "没有支付宝支付单");
        }
        if ("SUCCESS".equals(payment.getStatus())) {
            return toView(payment, null);
        }
        queryAndConfirm(payment);
        return toView(findByPaymentNo(payment.getPaymentNo()), null);
    }

    @Scheduled(fixedDelay = 20000)
    public void reconcilePendingAlipay() {
        if (!alipayGateway.isConfigured()) {
            return;
        }
        List<PaymentEntity> pending = paymentMapper.selectList(new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getChannel, "ALIPAY")
                .eq(PaymentEntity::getStatus, "PENDING")
                .ge(PaymentEntity::getCreatedAt, LocalDateTime.now().minusMinutes(20))
                .last("LIMIT 20"));
        for (PaymentEntity payment : pending) {
            try {
                queryAndConfirm(payment);
            } catch (RuntimeException ex) {
                log.warn("Reconcile payment {} failed", payment.getPaymentNo());
            }
        }
    }

    private void queryAndConfirm(PaymentEntity payment) {
        AlipayTradeQueryResponse response = alipayGateway.query(payment.getPaymentNo());
        if (response == null || !response.isSuccess()) {
            return;
        }
        String status = response.getTradeStatus();
        if ("TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status)) {
            if (!AlipayGateway.fenToYuan(payment.getAmountFen()).equals(response.getTotalAmount())) {
                return;
            }
            confirmPaid(payment, response.getTradeNo(), "query:" + status);
        }
    }

    private void confirmPaid(PaymentEntity payment, String tradeNo, String notifyRaw) {
        PaymentEntity latest = paymentMapper.selectById(payment.getId());
        if (latest == null || "SUCCESS".equals(latest.getStatus())) {
            return;
        }
        OrderEntity order = orderService.require(latest.getOrderId());
        if (!"PENDING".equals(order.getPayStatus()) && !"PAID".equals(order.getPayStatus())) {
            return;
        }
        latest.setChannelTradeNo(tradeNo);
        latest.setNotifyRaw(notifyRaw == null ? null : notifyRaw.substring(0, Math.min(notifyRaw.length(), 2000)));
        if ("PAID".equals(order.getPayStatus())) {
            latest.setStatus("SUCCESS");
            latest.setUpdatedAt(LocalDateTime.now());
            paymentMapper.updateById(latest);
            return;
        }
        fulfill(latest, order);
    }

    private void fulfill(PaymentEntity payment, OrderEntity order) {
        payment.setStatus("SUCCESS");
        if (payment.getChannelTradeNo() == null) {
            payment.setChannelTradeNo(payment.getPaymentNo());
        }
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        if (orderService.isNodeSubscription(order)) {
            nodeProvisioningService.enqueuePaidOrder(order);
            orderService.markPaidAndDeliver(order, "PROVISIONING");
            return;
        }

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

    private PaymentEntity findByPaymentNo(String paymentNo) {
        return paymentMapper.selectOne(
                new LambdaQueryWrapper<PaymentEntity>().eq(PaymentEntity::getPaymentNo, paymentNo));
    }

    private PaymentEntity latestAlipay(Long orderId) {
        return paymentMapper.selectOne(new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getOrderId, orderId)
                .eq(PaymentEntity::getChannel, "ALIPAY")
                .orderByDesc(PaymentEntity::getId)
                .last("LIMIT 1"));
    }

    private PaymentView toView(PaymentEntity payment, String paymentHtml) {
        return new PaymentView(
                payment.getId(),
                payment.getPaymentNo(),
                payment.getOrderId(),
                payment.getChannel(),
                payment.getAmountFen(),
                payment.getStatus(),
                paymentHtml);
    }
}
