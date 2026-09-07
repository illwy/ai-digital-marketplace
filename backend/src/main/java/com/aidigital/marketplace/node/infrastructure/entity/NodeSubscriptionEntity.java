package com.aidigital.marketplace.node.infrastructure.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("node_subscription")
public class NodeSubscriptionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private Long lastOrderId;
    private String clientName;
    private Long providerClientId;
    private Long trafficBytes;
    private Integer deviceLimit;
    private LocalDateTime expiresAt;
    private String subscriptionUrl;
    private String clashConfig;
    private String status;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getLastOrderId() { return lastOrderId; }
    public void setLastOrderId(Long lastOrderId) { this.lastOrderId = lastOrderId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public Long getProviderClientId() { return providerClientId; }
    public void setProviderClientId(Long providerClientId) { this.providerClientId = providerClientId; }
    public Long getTrafficBytes() { return trafficBytes; }
    public void setTrafficBytes(Long trafficBytes) { this.trafficBytes = trafficBytes; }
    public Integer getDeviceLimit() { return deviceLimit; }
    public void setDeviceLimit(Integer deviceLimit) { this.deviceLimit = deviceLimit; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getSubscriptionUrl() { return subscriptionUrl; }
    public void setSubscriptionUrl(String subscriptionUrl) { this.subscriptionUrl = subscriptionUrl; }
    public String getClashConfig() { return clashConfig; }
    public void setClashConfig(String clashConfig) { this.clashConfig = clashConfig; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
