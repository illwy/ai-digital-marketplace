package com.aidigital.marketplace.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecretRulesTest {

    @Test
    void rejectsShortOrPlaceholderJwt() {
        assertThatThrownBy(() -> SecretRules.verifyJwtSecret("short"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SecretRules.verifyJwtSecret("change-me-in-dev-only-32-chars-min-secret"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsUniqueJwt() {
        assertThatCode(() -> SecretRules.verifyJwtSecret("replace-this-dev-jwt-secret-key-min-32b"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsWellKnownAdminPassword() {
        assertThatThrownBy(() -> SecretRules.verifyAdminPassword("admin123456"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SecretRules.verifyAdminPassword("short"))
                .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> SecretRules.verifyAdminPassword("")).doesNotThrowAnyException();
        assertThatCode(() -> SecretRules.verifyAdminPassword("test-admin-password")).doesNotThrowAnyException();
    }
}
