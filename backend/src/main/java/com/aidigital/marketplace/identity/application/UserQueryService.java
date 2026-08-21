package com.aidigital.marketplace.identity.application;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.aidigital.marketplace.identity.infrastructure.entity.UserEntity;
import com.aidigital.marketplace.identity.infrastructure.mapper.UserMapper;
import com.aidigital.marketplace.identity.infrastructure.mapper.UserRoleMapper;
import com.aidigital.marketplace.identity.security.AuthUser;
import com.aidigital.marketplace.shared.web.ApiException;

@Service
public class UserQueryService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public UserQueryService(UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public AuthUser loadEnabled(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "未登录或令牌无效");
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_DISABLED", "账号已禁用");
        }
        List<String> roles = userRoleMapper.findRoleCodesByUserId(userId);
        return new AuthUser(user.getId(), user.getUsername(), user.getPasswordHash(), user.getNickname(), true, roles);
    }
}
