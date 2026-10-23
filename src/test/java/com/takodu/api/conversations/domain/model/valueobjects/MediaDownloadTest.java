package com.takodu.api.conversations.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaDownloadTest {
    private static final Instant NOW = Instant.parse("2026-10-22T15:00:00Z");

    @Test
    void contentWithMediaIsDueAtOnceAndTextHasNone() {
        var download = MediaDownload.of(new MessageContent(MessageKind.IMAGE, null, "123", "image/jpeg"), NOW);
        assertEquals(MediaStatus.PENDING, download.status());
        assertEquals(NOW, download.nextAttemptAt());
        assertNull(MediaDownload.of(MessageContent.text("Hola"), NOW));
    }

    @Test
    void storedFileIsReadyWithItsPath() {
        var ready = pending().stored("org/message");
        assertEquals(MediaStatus.READY, ready.status());
        assertEquals("org/message", ready.path());
        assertEquals(1, ready.attempts());
        assertNull(ready.nextAttemptAt());
    }

    @Test
    void retriesWaitOneTwoFourAndEightMinutesThenFail() {
        var download = pending();
        for (int attempt = 1; attempt <= 4; attempt++) {
            download = download.failed("Meta did not answer", true, NOW);
            assertEquals(MediaStatus.PENDING, download.status());
            assertEquals(NOW.plus(Duration.ofMinutes(1L << (attempt - 1))), download.nextAttemptAt());
        }
        download = download.failed("Meta did not answer", true, NOW);
        assertEquals(MediaStatus.FAILED, download.status());
        assertEquals(MediaDownload.MAX_ATTEMPTS, download.attempts());
        assertEquals("Meta did not answer", download.error());
        assertNull(download.nextAttemptAt());
    }

    @Test
    void failureThatWillRepeatEndsAtOnce() {
        var failed = pending().failed("The file is larger than WhatsApp allows", false, NOW);
        assertEquals(MediaStatus.FAILED, failed.status());
        assertEquals(1, failed.attempts());
    }

    @Test
    void settledDownloadIsNotTriedAgain() {
        assertThrows(IllegalStateException.class, () -> pending().stored("a").failed("x", true, NOW));
        assertThrows(IllegalStateException.class, () -> pending().failed("x", false, NOW).stored("a"));
    }

    @Test
    void limitsFollowMeta() {
        assertEquals(5L * 1024 * 1024, MediaDownload.maxBytes(MessageKind.IMAGE));
        assertEquals(16L * 1024 * 1024, MediaDownload.maxBytes(MessageKind.AUDIO));
        assertEquals(16L * 1024 * 1024, MediaDownload.maxBytes(MessageKind.VIDEO));
        assertEquals(512L * 1024, MediaDownload.maxBytes(MessageKind.STICKER));
        assertEquals(100L * 1024 * 1024, MediaDownload.maxBytes(MessageKind.DOCUMENT));
    }

    private static MediaDownload pending() {
        return MediaDownload.of(new MessageContent(MessageKind.AUDIO, null, "123", "audio/ogg"), NOW);
    }
}
