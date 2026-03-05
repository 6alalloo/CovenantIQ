package com.covenantiq.service;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Component
public class WebhookSecretCodec {

    private static final String VERSION_PREFIX = "v1";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_SIZE_BYTES = 12;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public WebhookSecretCodec(@Value("${app.webhook.encryption-key-base64}") String encryptionKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                throw new IllegalStateException("app.webhook.encryption-key-base64 must decode to 16, 24, or 32 bytes");
            }
            this.keySpec = new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("app.webhook.encryption-key-base64 must be valid Base64", ex);
        }
    }

    public String encode(String raw) {
        try {
            byte[] iv = new byte[IV_SIZE_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));

            return VERSION_PREFIX + ":"
                    + Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(ciphertext);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt webhook secret", ex);
        }
    }

    public String decode(String encoded) {
        if (encoded != null && encoded.startsWith(VERSION_PREFIX + ":")) {
            return decryptV1(encoded);
        }
        return decodeLegacyBase64(encoded);
    }

    private String decryptV1(String encoded) {
        String[] parts = encoded.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalStateException("Invalid webhook secret payload");
        }
        try {
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Unable to decrypt webhook secret", ex);
        }
    }

    private String decodeLegacyBase64(String encoded) {
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid webhook secret payload", ex);
        }
    }
}
