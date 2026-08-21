package com.aidigital.marketplace.wallet.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aidigital.marketplace.wallet.infrastructure.entity.WalletLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface WalletLogMapper extends BaseMapper<WalletLogEntity> {}
