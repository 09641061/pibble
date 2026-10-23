package com.takodu.api.conversations.application.internal.jobs;

import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.RecordingWhatsappGateway;
import com.takodu.api.shared.testing.TestSalon;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.takodu.api.conversations.MetaWebhooks.newWaId;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static com.takodu.api.conversations.TestInbox.reply;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every job claims its rows with {@code FOR UPDATE SKIP LOCKED}, so app instances running the same job share the
 * work. Each sender call is its own transaction on its own connection, as on another instance.
 */
@QuarkusTest
class OutboundMessageSenderTest {

    @Inject
    OutboundMessageSender sender;

    @Inject
    RecordingWhatsappGateway whatsapp;

    @Test
    void secondInstanceSendsTheNextMessageWhileTheFirstHoldsItsOwn() throws Exception {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var ana = newWaId();
        var beto = newWaId();
        var toAna = customerWrites(salon.owner(), salon.establishmentId(), line, ana, Instant.now());
        var toBeto = customerWrites(salon.owner(), salon.establishmentId(), line, beto, Instant.now());
        reply(salon.owner(), toAna, UUID.randomUUID(), "Hola Ana").statusCode(201);
        reply(salon.owner(), toBeto, UUID.randomUUID(), "Hola Beto").statusCode(201);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        whatsapp.holdFor(ana, entered, release);

        var first = CompletableFuture.runAsync(() -> {
            for (int run = 0; run < 50 && entered.getCount() > 0; run++) sender.sendDue();
        });
        assertTrue(entered.await(20, TimeUnit.SECONDS), "the first instance reached Ana's message");

        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            for (int run = 0; run < 50 && whatsapp.sentTo(beto).isEmpty(); run++) sender.sendDue();
        });
        assertEquals(List.of("Hola Beto"), whatsapp.sentTo(beto));
        assertEquals(List.of(), whatsapp.sentTo(ana));

        release.countDown();
        first.get(20, TimeUnit.SECONDS);
        sender.sendDue();
        assertEquals(List.of("Hola Ana"), whatsapp.sentTo(ana));
    }
}
