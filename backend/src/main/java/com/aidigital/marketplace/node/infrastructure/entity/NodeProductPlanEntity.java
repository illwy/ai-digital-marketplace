package com.aidigital.marketplace.node.infrastructure.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("node_product_plan")
public class NodeProductPlanEntity {
    @TableId
    private Long productId;
    private String apiBaseUrl;
    private String webPath;
    private String inboundIdsJson;
    private Long trafficBytes;
    private Integer durationDays;
    private Integer deviceLimit;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getWebPath() { return webPath; }
    public void setWebPath(String webPath) { this.webPath = webPath; }
    public String getInboundIdsJson() { return inboundIdsJson; }
    public void setInboundIdsJson(String inboundIdsJson) { this.inboundIdsJson = inboundIdsJson; }
    public Long getTrafficBytes() { return trafficBytes; }
    public void setTrafficBytes(Long trafficBytes) { this.trafficBytes = trafficBytes; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public Integer getDeviceLimit() { return deviceLimit; }
    public void setDeviceLimit(Integer deviceLimit) { this.deviceLimit = deviceLimit; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
