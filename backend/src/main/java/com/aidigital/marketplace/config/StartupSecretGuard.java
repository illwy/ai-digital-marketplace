package com.aidigital.marketplace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartupSecretGuard implements ApplicationRunner {

    private final String jwtSecret;
    private final String adminPassword;

    public StartupSecretGuard(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.admin.password:}") String adminPassword) {
        this.jwtSecret = jwtSecret;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        SecretRules.verifyJwtSecret(jwtSecret);
        SecretRules.verifyAdminPassword(adminPassword);
    }
}
