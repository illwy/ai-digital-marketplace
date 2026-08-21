package com.aidigital.marketplace.identity.application;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.config.SecretRules;
import com.aidigital.marketplace.identity.infrastructure.entity.UserEntity;
import com.aidigital.marketplace.identity.infrastructure.mapper.UserMapper;
import com.aidigital.marketplace.wallet.application.WalletService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Component
public class AdminUserSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeed.class);

    private final UserMapper userMapper;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final String username;
    private final String password;

    public AdminUserSeed(
            UserMapper userMapper,
            AuthService authService,
            PasswordEncoder passwordEncoder,
            WalletService walletService,
            @Value("${app.admin.username}") String username,
            @Value("${app.admin.password:}") String password) {
        this.userMapper = userMapper;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.walletService = walletService;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            log.warn("ADMIN_PASSWORD is empty; skip admin seed");
            return;
        }
        SecretRules.verifyAdminPassword(password);
        UserEntity existing =
                userMapper.selectOne(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
        if (existing != null) {
            return;
        }
        UserEntity admin = new UserEntity();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setNickname("管理员");
        admin.setStatus("ENABLED");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(admin);
        authService.assignRole(admin.getId(), "ADMIN");
        walletService.createForUser(admin.getId());
        log.info("Seeded admin user {}", username);
    }
}
