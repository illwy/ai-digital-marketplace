package com.aidigital.marketplace.payment.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.aidigital.marketplace.payment.config.AlipayProperties;
import com.aidigital.marketplace.payment.infrastructure.entity.PaymentEntity;
import com.aidigital.marketplace.shared.web.ApiException;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.GoodsDetail;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;

@Component
public class AlipayGateway {

    private static final Logger log = LoggerFactory.getLogger(AlipayGateway.class);
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlipayProperties properties;
    private final AlipayClient alipayClient;

    public AlipayGateway(AlipayProperties properties, ObjectProvider<AlipayClient> alipayClient) {
        this.properties = properties;
        this.alipayClient = alipayClient.getIfAvailable();
    }

    public boolean isConfigured() {
        return properties.isConfigured() && alipayClient != null;
    }

    public String pagePayForm(OrderEntity order, PaymentEntity payment, String subject, Long productId) {
        requireClient();
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(properties.notifyUrlOrDefault());
        request.setReturnUrl(properties.returnUrl(order.getId()));

        String safeSubject = sanitizeSubject(subject);
        String amount = fenToYuan(payment.getAmountFen());
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(payment.getPaymentNo());
        model.setTotalAmount(amount);
        model.setSubject(safeSubject);
        model.setBody(sanitizeBody("钥市数字商品自动交付。订单 " + order.getOrderNo() + "，付款后立刻出卡。"));
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        model.setQrPayMode("4");
        model.setQrcodeWidth(220L);
        model.setIntegrationType("PCWEB");
        GoodsDetail goods = new GoodsDetail();
        goods.setGoodsId(productId == null ? "digital" : String.valueOf(productId));
        goods.setGoodsName(safeSubject);
        goods.setQuantity(1L);
        goods.setPrice(amount);
        goods.setBody("数字商品，付款后立刻出卡");
        model.setGoodsDetail(List.of(goods));
        if (order.getExpireAt() != null) {
            model.setTimeExpire(order.getExpireAt().format(ALIPAY_TIME));
        }
        request.setBizModel(model);

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request, "POST");
            if (response == null || response.getBody() == null || response.getBody().isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "ALIPAY_CREATE_FAILED", "支付宝下单失败，请稍后重试");
            }
            return response.getBody();
        } catch (AlipayApiException ex) {
            log.warn("Alipay page pay failed: {}", ex.getErrMsg());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ALIPAY_CREATE_FAILED", "支付宝下单失败，请稍后重试");
        }
    }

    public AlipayTradeQueryResponse query(String paymentNo) {
        requireClient();
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(paymentNo);
        request.setBizModel(model);
        try {
            return alipayClient.execute(request);
        } catch (AlipayApiException ex) {
            log.warn("Alipay query failed: {}", ex.getErrMsg());
            return null;
        }
    }

    public boolean verifyNotify(Map<String, String> params) {
        if (!isConfigured()) {
            return false;
        }
        try {
            return AlipaySignature.rsaCheckV1(params, properties.getAlipayPublicKey(), "UTF-8", "RSA2");
        } catch (AlipayApiException ex) {
            log.warn("Alipay notify verify failed: {}", ex.getErrMsg());
            return false;
        }
    }

    public boolean matchesAppId(String appId) {
        return properties.getAppId() != null && properties.getAppId().equals(appId);
    }

    public boolean matchesSeller(String sellerId) {
        return properties.getSellerId() == null
                || properties.getSellerId().isBlank()
                || properties.getSellerId().equals(sellerId);
    }

    public static String fenToYuan(int amountFen) {
        return BigDecimal.valueOf(amountFen).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    public static String sanitizeSubject(String subject) {
        return sanitizeAlipayText(subject, "钥市数字商品", 256);
    }

    public static String sanitizeBody(String body) {
        return sanitizeAlipayText(body, "钥市数字商品，付款后立刻出卡", 128);
    }

    private static String sanitizeAlipayText(String value, String fallback, int maxLen) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String cleaned = value.replace("/", " ").replace("=", " ").replace("&", " ").trim();
        if (cleaned.isEmpty()) {
            return fallback;
        }
        return cleaned.length() <= maxLen ? cleaned : cleaned.substring(0, maxLen);
    }

    private void requireClient() {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ALIPAY_NOT_CONFIGURED", "支付宝尚未配置");
        }
    }
}
