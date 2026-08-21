package com.aidigital.marketplace.identity.application;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.identity.infrastructure.entity.RoleEntity;
import com.aidigital.marketplace.identity.infrastructure.entity.UserEntity;
import com.aidigital.marketplace.identity.infrastructure.entity.UserRoleEntity;
import com.aidigital.marketplace.identity.infrastructure.mapper.RoleMapper;
import com.aidigital.marketplace.identity.infrastructure.mapper.UserMapper;
import com.aidigital.marketplace.identity.infrastructure.mapper.UserRoleMapper;
import com.aidigital.marketplace.identity.security.AuthUser;
import com.aidigital.marketplace.identity.security.JwtService;
import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.wallet.application.WalletService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.jsonwebtoken.Claims;

@Service
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "marketplace:refresh:";
    private static final String LOGIN_RL_PREFIX = "marketplace:login:rl:";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redis;
    private final UserQueryService userQueryService;
    private final WalletService walletService;

    public AuthService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            StringRedisTemplate redis,
            UserQueryService userQueryService,
            WalletService walletService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redis = redis;
        this.userQueryService = userQueryService;
        this.walletService = walletService;
    }

    @Transactional
    public TokenBundle register(String username, String password, String nickname) {
        UserEntity existing = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (existing != null) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "用户名已存在");
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
        user.setStatus("ENABLED");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        assignRole(user.getId(), "USER");
        walletService.createForUser(user.getId());
        return issue(userQueryService.loadEnabled(user.getId()));
    }

    public TokenBundle login(String username, String password) {
        String rlKey = LOGIN_RL_PREFIX + username;
        String attempts = redis.opsForValue().get(rlKey);
        if (attempts != null && Integer.parseInt(attempts) >= 10) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_RATE_LIMITED", "登录尝试过多，请稍后再试");
        }
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            redis.opsForValue().increment(rlKey);
            redis.expire(rlKey, Duration.ofSeconds(600));
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "用户名或密码错误");
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_DISABLED", "账号已禁用");
        }
        redis.delete(rlKey);
        return issue(userQueryService.loadEnabled(user.getId()));
    }

    public TokenBundle refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "刷新令牌无效");
        }
        if (!"refresh".equals(claims.get("typ"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "刷新令牌无效");
        }
        String jti = claims.getId();
        String stored = redis.opsForValue().get(REFRESH_KEY_PREFIX + jti);
        if (stored == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "刷新令牌已失效");
        }
        redis.delete(REFRESH_KEY_PREFIX + jti);
        Long userId = Long.valueOf(claims.getSubject());
        return issue(userQueryService.loadEnabled(userId));
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtService.parse(refreshToken);
            String jti = claims.getId();
            if (jti != null) {
                redis.delete(REFRESH_KEY_PREFIX + jti);
            }
        } catch (RuntimeException ignored) {
            // already invalid
        }
    }

    public TokenBundle issue(AuthUser user) {
        String access = jwtService.createAccessToken(user);
        String refresh = jwtService.createRefreshToken(user);
        Claims refreshClaims = jwtService.parse(refresh);
        redis.opsForValue().set(
                REFRESH_KEY_PREFIX + refreshClaims.getId(),
                String.valueOf(user.getId()),
                Duration.ofSeconds(jwtService.getRefreshTtlSeconds()));
        return new TokenBundle(access, refresh, jwtService.getAccessTtlSeconds(), user);
    }

    public void assignRole(Long userId, String roleCode) {
        RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, roleCode));
        if (role == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ROLE_MISSING", "角色未初始化");
        }
        UserRoleEntity link = new UserRoleEntity();
        link.setUserId(userId);
        link.setRoleId(role.getId());
        userRoleMapper.insert(link);
    }

    public record TokenBundle(String accessToken, String refreshToken, long expiresIn, AuthUser user) {}
}
