package com.takodu.api.conversations.infrastructure.meta;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaSignatureVerifierTest {
    private final MetaSignatureVerifier verifier = new MetaSignatureVerifier("app-secret");
    private final byte[] body = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);

    @Test
    void acceptsTheHmacOfTheRawBody() {
        assertTrue(verifier.verify(body, verifier.header(body)));
    }

    @Test
    void refusesAnotherSecretAChangedBodyOrGarbage() {
        assertFalse(verifier.verify(body, new MetaSignatureVerifier("other").header(body)));
        assertFalse(verifier.verify("{}".getBytes(StandardCharsets.UTF_8), verifier.header(body)));
        assertFalse(verifier.verify(body, "sha256=zz"));
        assertFalse(verifier.verify(body, null));
    }
}
