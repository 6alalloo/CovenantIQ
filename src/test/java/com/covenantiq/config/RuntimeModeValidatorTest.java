package com.covenantiq.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeModeValidatorTest {

    @Test
    void rejectsPlaceholderSecretsWhenStrictValidationIsEnabled() {
        AppModeProperties properties = new AppModeProperties();

        RuntimeModeValidator validator = new RuntimeModeValidator(
                properties,
                RuntimeModeValidator.PLACEHOLDER_JWT_SECRET,
                RuntimeModeValidator.FALLBACK_WEBHOOK_KEY_BASE64
        );

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void allowsPlaceholderSecretsWhenDemoModeIsEnabled() {
        AppModeProperties properties = new AppModeProperties();
        properties.setDemoEnabled(true);

        RuntimeModeValidator validator = new RuntimeModeValidator(
                properties,
                RuntimeModeValidator.PLACEHOLDER_JWT_SECRET,
                RuntimeModeValidator.FALLBACK_WEBHOOK_KEY_BASE64
        );

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void rejectsConflictingDemoAndTestModes() {
        AppModeProperties properties = new AppModeProperties();
        properties.setDemoEnabled(true);
        properties.setTestEnabled(true);

        RuntimeModeValidator validator = new RuntimeModeValidator(
                properties,
                RuntimeModeValidator.PLACEHOLDER_JWT_SECRET,
                RuntimeModeValidator.FALLBACK_WEBHOOK_KEY_BASE64
        );

        assertThrows(IllegalStateException.class, validator::validate);
    }
}
