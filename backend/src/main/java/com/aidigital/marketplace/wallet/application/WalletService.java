package com.aidigital.marketplace.wallet.application;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.wallet.api.dto.WalletView;
import com.aidigital.marketplace.wallet.infrastructure.entity.WalletEntity;
import com.aidigital.marketplace.wallet.infrastructure.entity.WalletLogEntity;
import com.aidigital.marketplace.wallet.infrastructure.mapper.WalletLogMapper;
import com.aidigital.marketplace.wallet.infrastructure.mapper.WalletMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class WalletService {

    private final WalletMapper walletMapper;
    private final WalletLogMapper walletLogMapper;

    public WalletService(WalletMapper walletMapper, WalletLogMapper walletLogMapper) {
        this.walletMapper = walletMapper;
        this.walletLogMapper = walletLogMapper;
    }

    @Transactional
    public void createForUser(Long userId) {
        if (find(userId) != null) {
            return;
        }
        WalletEntity wallet = new WalletEntity();
        wallet.setUserId(userId);
        wallet.setBalanceFen(0);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletMapper.insert(wallet);
    }

    public WalletView get(Long userId) {
        WalletEntity wallet = require(userId);
        return new WalletView(wallet.getUserId(), wallet.getBalanceFen());
    }

    @Transactional
    public WalletView credit(Long userId, int amountFen, String type, String bizNo, String remark) {
        createForUser(userId);
        WalletEntity before = require(userId);
        int updated = walletMapper.credit(userId, amountFen);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "WALLET_CREDIT_FAILED", "钱包入账失败");
        }
        WalletEntity after = require(userId);
        writeLog(after, amountFen, before.getBalanceFen(), after.getBalanceFen(), type, bizNo, remark);
        return new WalletView(userId, after.getBalanceFen());
    }

    @Transactional
    public WalletView debit(Long userId, int amountFen, String bizNo, String remark) {
        createForUser(userId);
        WalletEntity before = require(userId);
        int updated = walletMapper.debit(userId, amountFen);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "WALLET_INSUFFICIENT", "钱包余额不足");
        }
        WalletEntity after = require(userId);
        writeLog(after, -amountFen, before.getBalanceFen(), after.getBalanceFen(), "DEBIT", bizNo, remark);
        return new WalletView(userId, after.getBalanceFen());
    }

    private void writeLog(
            WalletEntity wallet,
            int changeFen,
            int before,
            int after,
            String type,
            String bizNo,
            String remark) {
        WalletLogEntity log = new WalletLogEntity();
        log.setWalletId(wallet.getId());
        log.setUserId(wallet.getUserId());
        log.setChangeFen(changeFen);
        log.setBalanceBeforeFen(before);
        log.setBalanceAfterFen(after);
        log.setType(type);
        log.setBizNo(bizNo);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        walletLogMapper.insert(log);
    }

    private WalletEntity require(Long userId) {
        WalletEntity wallet = find(userId);
        if (wallet == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "钱包不存在");
        }
        return wallet;
    }

    private WalletEntity find(Long userId) {
        return walletMapper.selectOne(new LambdaQueryWrapper<WalletEntity>().eq(WalletEntity::getUserId, userId));
    }
}
