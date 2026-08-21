package com.aidigital.marketplace.payment.infrastructure.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("payment")
public class PaymentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String paymentNo;
    private Long orderId;
    private String channel;
    private Integer amountFen;
    private String status;
    private String channelTradeNo;
    private String notifyRaw;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Integer getAmountFen() {
        return amountFen;
    }

    public void setAmountFen(Integer amountFen) {
        this.amountFen = amountFen;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChannelTradeNo() {
        return channelTradeNo;
    }

    public void setChannelTradeNo(String channelTradeNo) {
        this.channelTradeNo = channelTradeNo;
    }

    public String getNotifyRaw() {
        return notifyRaw;
    }

    public void setNotifyRaw(String notifyRaw) {
        this.notifyRaw = notifyRaw;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
