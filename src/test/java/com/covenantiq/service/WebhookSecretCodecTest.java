package com.covenantiq.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSecretCodecTest {

    private static final String TEST_KEY_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void encodeAndDecodeRoundTripUsesVersionedCiphertext() {
        WebhookSecretCodec codec = new WebhookSecretCodec(TEST_KEY_BASE64);
        String raw = "super-secret-value";

        String encoded = codec.encode(raw);

        assertTrue(encoded.startsWith("v1:"));
        assertNotEquals(raw, encoded);
        assertEquals(raw, codec.decode(encoded));
    }

    @Test
    void decodeSupportsLegacyBase64Secrets() {
        WebhookSecretCodec codec = new WebhookSecretCodec(TEST_KEY_BASE64);
        String raw = "legacy-secret";
        String legacyEncoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));

        assertEquals(raw, codec.decode(legacyEncoded));
    }

    @Test
    void encodeUsesRandomIvSoCiphertextDiffersForSameInput() {
        WebhookSecretCodec codec = new WebhookSecretCodec(TEST_KEY_BASE64);
        String raw = "same-input";

        String encoded1 = codec.encode(raw);
        String encoded2 = codec.encode(raw);

        assertNotEquals(encoded1, encoded2);
        assertEquals(raw, codec.decode(encoded1));
        assertEquals(raw, codec.decode(encoded2));
    }
}
