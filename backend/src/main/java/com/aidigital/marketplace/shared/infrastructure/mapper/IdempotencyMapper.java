package com.aidigital.marketplace.shared.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aidigital.marketplace.shared.infrastructure.entity.IdempotencyEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface IdempotencyMapper extends BaseMapper<IdempotencyEntity> {}
