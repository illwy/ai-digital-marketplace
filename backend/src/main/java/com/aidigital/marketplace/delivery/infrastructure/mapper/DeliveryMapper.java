package com.aidigital.marketplace.delivery.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aidigital.marketplace.delivery.infrastructure.entity.DeliveryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface DeliveryMapper extends BaseMapper<DeliveryEntity> {}
