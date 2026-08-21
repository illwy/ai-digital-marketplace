package com.aidigital.marketplace.wallet.infrastructure.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("wallet_log")
public class WalletLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long walletId;
    private Long userId;
    private Integer changeFen;
    private Integer balanceBeforeFen;
    private Integer balanceAfterFen;
    private String type;
    private String bizNo;
    private String remark;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getChangeFen() {
        return changeFen;
    }

    public void setChangeFen(Integer changeFen) {
        this.changeFen = changeFen;
    }

    public Integer getBalanceBeforeFen() {
        return balanceBeforeFen;
    }

    public void setBalanceBeforeFen(Integer balanceBeforeFen) {
        this.balanceBeforeFen = balanceBeforeFen;
    }

    public Integer getBalanceAfterFen() {
        return balanceAfterFen;
    }

    public void setBalanceAfterFen(Integer balanceAfterFen) {
        this.balanceAfterFen = balanceAfterFen;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
