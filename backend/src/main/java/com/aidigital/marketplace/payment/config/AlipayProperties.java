package com.aidigital.marketplace.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.alipay")
public class AlipayProperties {

    private String appId = "";
    private String privateKey = "";
    private String alipayPublicKey = "";
    private String serverUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private String notifyUrl = "";
    private String returnUrlBase = "http://localhost:8088";
    private String sellerId = "";

    public boolean isConfigured() {
        return !isBlank(appId) && !isBlank(privateKey) && !isBlank(alipayPublicKey);
    }

    public String notifyUrlOrDefault() {
        if (!isBlank(notifyUrl)) {
            return notifyUrl.trim();
        }
        return trimSlash(returnUrlBase) + "/api/v1/payments/notify/alipay";
    }

    public String returnUrl(Long orderId) {
        return trimSlash(returnUrlBase) + "/orders/" + orderId + "?from=alipay";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8088";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getReturnUrlBase() {
        return returnUrlBase;
    }

    public void setReturnUrlBase(String returnUrlBase) {
        this.returnUrlBase = returnUrlBase;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
}
