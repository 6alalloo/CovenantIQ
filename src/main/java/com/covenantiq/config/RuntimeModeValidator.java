package com.covenantiq.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RuntimeModeValidator {

    static final String PLACEHOLDER_JWT_SECRET = "change-this-secret-for-prod-change-this-secret";
    static final String FALLBACK_WEBHOOK_KEY_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final AppModeProperties appModeProperties;
    private final String jwtSecret;
    private final String webhookEncryptionKeyBase64;

    public RuntimeModeValidator(
            AppModeProperties appModeProperties,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.webhook.encryption-key-base64}") String webhookEncryptionKeyBase64
    ) {
        this.appModeProperties = appModeProperties;
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret.trim();
        this.webhookEncryptionKeyBase64 = webhookEncryptionKeyBase64 == null ? "" : webhookEncryptionKeyBase64.trim();
    }

    @PostConstruct
    void validate() {
        if (appModeProperties.isDemoEnabled() && appModeProperties.isTestEnabled()) {
            throw new IllegalStateException("app.mode.demo-enabled and app.mode.test-enabled cannot both be true");
        }
        if (!appModeProperties.strictSecretValidationEnabled()) {
            return;
        }
        if (jwtSecret.isBlank() || PLACEHOLDER_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("A non-placeholder app.jwt.secret is required when demo/test mode is disabled");
        }
        if (webhookEncryptionKeyBase64.isBlank() || FALLBACK_WEBHOOK_KEY_BASE64.equals(webhookEncryptionKeyBase64)) {
            throw new IllegalStateException(
                    "A non-fallback app.webhook.encryption-key-base64 is required when demo/test mode is disabled"
            );
        }
    }
}
