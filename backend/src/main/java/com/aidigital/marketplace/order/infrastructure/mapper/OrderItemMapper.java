package com.aidigital.marketplace.order.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aidigital.marketplace.order.infrastructure.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {}
