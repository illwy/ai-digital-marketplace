package com.aidigital.marketplace.payment.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aidigital.marketplace.payment.infrastructure.entity.PaymentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface PaymentMapper extends BaseMapper<PaymentEntity> {}
