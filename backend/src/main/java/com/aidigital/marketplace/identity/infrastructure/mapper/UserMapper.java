package com.aidigital.marketplace.identity.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aidigital.marketplace.identity.infrastructure.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
