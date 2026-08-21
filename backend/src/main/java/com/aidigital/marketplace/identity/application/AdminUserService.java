package com.aidigital.marketplace.identity.application;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.identity.api.dto.AdminUserView;
import com.aidigital.marketplace.identity.infrastructure.entity.UserEntity;
import com.aidigital.marketplace.identity.infrastructure.mapper.UserMapper;
import com.aidigital.marketplace.identity.infrastructure.mapper.UserRoleMapper;
import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.shared.web.ListResponse;
import com.aidigital.marketplace.shared.web.PageQuery;
import com.aidigital.marketplace.shared.web.Pagination;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
public class AdminUserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public AdminUserService(UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public ListResponse<AdminUserView> list(String username, String status, int page, int pageSize) {
        Page<UserEntity> mp = PageQuery.of(page, pageSize);
        LambdaQueryWrapper<UserEntity> query = new LambdaQueryWrapper<UserEntity>()
                .like(username != null && !username.isBlank(), UserEntity::getUsername, username)
                .eq(status != null && !status.isBlank(), UserEntity::getStatus, status)
                .orderByDesc(UserEntity::getId);
        Page<UserEntity> result = userMapper.selectPage(mp, query);
        List<AdminUserView> items = result.getRecords().stream().map(this::toView).toList();
        return new ListResponse<>(
                items, Pagination.of((int) result.getCurrent(), (int) result.getSize(), result.getTotal()));
    }

    @Transactional
    public AdminUserView updateStatus(Long id, String status) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "用户不存在");
        }
        List<String> roles = userRoleMapper.findRoleCodesByUserId(id);
        if (roles.contains("ADMIN") && "DISABLED".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN_CANNOT_DISABLE", "不能禁用管理员");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toView(user);
    }

    private AdminUserView toView(UserEntity user) {
        return new AdminUserView(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getStatus(),
                userRoleMapper.findRoleCodesByUserId(user.getId()),
                user.getCreatedAt());
    }
}
