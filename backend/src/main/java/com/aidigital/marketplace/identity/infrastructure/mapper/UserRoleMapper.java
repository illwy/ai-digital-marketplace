package com.aidigital.marketplace.identity.infrastructure.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.aidigital.marketplace.identity.infrastructure.entity.UserRoleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {

    @Select("""
            SELECT r.code
            FROM user_role ur
            JOIN role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<String> findRoleCodesByUserId(Long userId);
}
