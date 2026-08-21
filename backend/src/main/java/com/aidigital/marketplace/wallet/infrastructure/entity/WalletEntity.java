package com.aidigital.marketplace.wallet.infrastructure.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("wallet")
public class WalletEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer balanceFen;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getBalanceFen() {
        return balanceFen;
    }

    public void setBalanceFen(Integer balanceFen) {
        this.balanceFen = balanceFen;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
