package com.takodu.api.conversations.infrastructure.meta;

import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappMediaException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaWhatsappMediaGatewayTest {

    @Test
    void downloadUpToTheLimitIsRead() throws IOException {
        try (var in = new MetaWhatsappMediaGateway.Limited(new ByteArrayInputStream(new byte[10]), 10)) {
            assertArrayEquals(new byte[10], in.readAllBytes());
        }
    }

    @Test
    void downloadPastTheLimitStopsAndWillNotBeRetried() {
        var in = new MetaWhatsappMediaGateway.Limited(new ByteArrayInputStream(new byte[11]), 10);
        var e = assertThrows(WhatsappMediaException.class, in::readAllBytes);
        assertFalse(e.retryable());
    }

    @Test
    void expiredUrlAndOutagesAreRetriedButRefusalsAreNot() {
        assertTrue(MetaWhatsappMediaGateway.retryable(404));
        assertTrue(MetaWhatsappMediaGateway.retryable(429));
        assertTrue(MetaWhatsappMediaGateway.retryable(503));
        assertFalse(MetaWhatsappMediaGateway.retryable(400));
        assertFalse(MetaWhatsappMediaGateway.retryable(401));
    }
}
