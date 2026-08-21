package com.aidigital.marketplace.identity.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aidigital.marketplace.identity.infrastructure.entity.RoleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
}
