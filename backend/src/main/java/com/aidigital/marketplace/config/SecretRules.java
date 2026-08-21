package com.aidigital.marketplace.config;

public final class SecretRules {

    private SecretRules() {}

    public static void verifyJwtSecret(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        String lower = secret.toLowerCase();
        if (lower.contains("change-me") || lower.contains("in-dev-only-32-chars")) {
            throw new IllegalStateException("JWT_SECRET is a placeholder; set a unique secret");
        }
    }

    public static void verifyAdminPassword(String password) {
        if (password == null || password.isBlank()) {
            return;
        }
        if (password.length() < 10) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 10 characters");
        }
        if ("admin123456".equals(password) || "password".equalsIgnoreCase(password)) {
            throw new IllegalStateException("ADMIN_PASSWORD is a well-known default; set a unique password");
        }
    }
}
