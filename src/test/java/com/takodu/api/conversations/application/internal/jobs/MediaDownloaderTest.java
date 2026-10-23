package com.takodu.api.conversations.application.internal.jobs;

import com.takodu.api.conversations.MetaWebhooks;
import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.RecordingWhatsappMediaGateway;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.takodu.api.conversations.ConversationRows.messagesWithWamid;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Media arrives through the recorded webhook and is downloaded by the job, as in production. */
@QuarkusTest
class MediaDownloaderTest {
    private static final byte[] PHOTO = "a photo of a haircut".getBytes(StandardCharsets.UTF_8);

    @Inject
    TestClock clock;

    @Inject
    MediaDownloader downloader;

    @Inject
    RecordingWhatsappMediaGateway media;

    @ConfigProperty(name = "takodu.media.dir")
    Path mediaDir;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void customersPhotoIsDownloadedToTheMediaDirectory() throws IOException {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var mediaId = newMediaId();
        media.serve(mediaId, PHOTO);
        var wamid = customerSendsPhoto(line, mediaId);
        assertEquals("pending", row(wamid).get("media_status"));

        downloadUntilSettled(wamid);

        var row = row(wamid);
        assertEquals("ready", row.get("media_status"));
        assertEquals("image/jpeg", row.get("media_mime"));
        assertEquals(salon.owner().organizationId() + "/" + row.get("id"), row.get("media_path"));
        assertArrayEquals(PHOTO, Files.readAllBytes(mediaDir.resolve((String) row.get("media_path"))));
        assertEquals(1, media.downloadsOf(mediaId));
    }

    @Test
    void failedDownloadIsTriedAgainLaterAndStopsAtTheFifthAttempt() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var mediaId = newMediaId();
        media.serve(mediaId, PHOTO);
        media.failFor(mediaId, 5, "Meta did not answer", true);
        var wamid = customerSendsPhoto(line, mediaId);

        downloadUntilSettled(wamid);
        assertEquals("pending", row(wamid).get("media_status"));
        assertEquals((short) 1, row(wamid).get("media_attempts"));

        for (int retry = 1; retry <= 4; retry++) {
            clock.advance(Duration.ofMinutes(1L << (retry - 1)).plusSeconds(1));
            downloadUntilSettled(wamid);
        }

        var row = row(wamid);
        assertEquals("failed", row.get("media_status"));
        assertEquals((short) 5, row.get("media_attempts"));
        assertEquals("Meta did not answer", row.get("media_error"));
        assertNull(row.get("media_path"));
        assertEquals(5, media.downloadsOf(mediaId));
        clock.advance(Duration.ofHours(1));
        downloader.downloadDue();
        assertEquals(5, media.downloadsOf(mediaId));
    }

    @Test
    void fileOverTheLimitFailsAtOnce() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var mediaId = newMediaId();
        media.serve(mediaId, new byte[5 * 1024 * 1024 + 1]);
        var wamid = customerSendsPhoto(line, mediaId);

        downloadUntilSettled(wamid);

        assertEquals("failed", row(wamid).get("media_status"));
        assertEquals("The file is larger than WhatsApp allows", row(wamid).get("media_error"));
    }

    @Test
    void neitherTheCaptionNorTheNumberIsLogged() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var mediaId = newMediaId();
        media.failFor(mediaId, 1, "Meta refused the media: HTTP 500", true);
        media.serve(mediaId, PHOTO);
        var waId = MetaWebhooks.newWaId();
        var logged = new CopyOnWriteArrayList<String>();
        var handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(String.valueOf(record.getMessage()) + " " + List.of(
                        record.getParameters() == null ? new Object[0] : record.getParameters()));
            }

            @Override
            public void flush() { }

            @Override
            public void close() { }
        };
        var root = Logger.getLogger("");
        root.addHandler(handler);
        try {
            var wamid = customerSendsPhoto(line, mediaId, waId);
            downloadUntilSettled(wamid);
            clock.advance(Duration.ofMinutes(2));
            downloadUntilSettled(wamid);
            assertEquals("ready", row(wamid).get("media_status"));
        } finally {
            root.removeHandler(handler);
        }
        assertFalse(logged.isEmpty(), "the failed attempt is logged");
        assertFalse(logged.stream().anyMatch(line1 -> line1.contains("Así quiero") || line1.contains(waId)),
                () -> "a log line leaked the caption or the number: " + logged);
    }

    private static String newMediaId() {
        return "media-" + UUID.randomUUID();
    }

    private static String customerSendsPhoto(String line, String mediaId) {
        return customerSendsPhoto(line, mediaId, MetaWebhooks.newWaId());
    }

    private static String customerSendsPhoto(String line, String mediaId, String waId) {
        var wamid = MetaWebhooks.newWamid();
        MetaWebhooks.post(MetaWebhooks.payload("image-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId,
                "WAMID", wamid, "MEDIA_ID", mediaId))).statusCode(200);
        return wamid;
    }

    /** Media other tests left pending is due too, so the job runs until this message's attempt is recorded. */
    private void downloadUntilSettled(String wamid) {
        var attempts = row(wamid).get("media_attempts");
        for (int run = 0; run < 50 && attempts.equals(row(wamid).get("media_attempts")); run++)
            downloader.downloadDue();
    }

    private static Map<String, Object> row(String wamid) {
        return messagesWithWamid(wamid).getFirst();
    }
}
