package com.takodu.api.conversations.domain.model.aggregates;

import com.takodu.api.conversations.domain.exceptions.LineAlreadyConnectedException;
import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsappLineTest {
    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    @Test
    void connectedLineCanBeDisconnectedTwice() {
        var line = connect();

        line.disconnect();
        line.disconnect();

        assertFalse(line.isConnected());
    }

    @Test
    void reconnectingTakesTheNewTokenAndKey() {
        var line = connect();
        line.disconnect();
        var key = new IdempotencyKey(UUID.randomUUID());

        line.reconnect("102290129340398", "+51 1 555 0100", new EncryptedToken("new"), LineMode.COEXISTENCE,
                NOW.plusSeconds(60), key);

        assertTrue(line.isConnected());
        assertEquals("new", line.getAccessToken().value());
        assertTrue(line.alreadyApplied(key));
    }

    @Test
    void connectedLineIsNotConnectedAgain() {
        var line = connect();

        assertThrows(LineAlreadyConnectedException.class, () -> line.reconnect("1", "+51", new EncryptedToken("x"),
                LineMode.COEXISTENCE, NOW, new IdempotencyKey(UUID.randomUUID())));
    }

    @Test
    void tokenNeverShowsInText() {
        assertFalse(new EncryptedToken("ciphertext").toString().contains("ciphertext"));
    }

    private static WhatsappLine connect() {
        return WhatsappLine.connect(new OrganizationId(UUID.randomUUID()), new EstablishmentId(UUID.randomUUID()),
                "102290129340398", "106540352242922", "+51 1 555 0100", new EncryptedToken("token"),
                LineMode.COEXISTENCE, NOW, new IdempotencyKey(UUID.randomUUID()));
    }
}
