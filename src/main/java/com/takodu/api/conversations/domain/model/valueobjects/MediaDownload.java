package com.takodu.api.conversations.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Where the download of a message's image, audio, video, document or sticker stands. Meta keeps only an id; the media
 * job fetches the file and stores it on Takodu's disk.
 *
 * @param path where the file is stored, relative to the media directory; set when {@code READY}, and kept while
 *        {@code REMOVING} until the file is deleted
 * @param attempts the downloads tried so far
 * @param nextAttemptAt when the job may try again; set only while {@code PENDING}
 * @param error why the last attempt failed, without the url or the content
 */
public record MediaDownload(MediaStatus status, String path, int attempts, Instant nextAttemptAt, String error) {
    /** A download is tried at most this many times, then the message shows its media as failed. */
    public static final int MAX_ATTEMPTS = 5;
    /** The first retry waits this long; each next one waits twice as long (1, 2, 4, 8 minutes). */
    public static final Duration FIRST_RETRY = Duration.ofMinutes(1);

    private static final long MB = 1024L * 1024L;

    public MediaDownload {
        Objects.requireNonNull(status, "status is required");
        if (status == MediaStatus.READY && path == null) throw new IllegalArgumentException("Downloaded media has a path");
        if (path != null && status != MediaStatus.READY && status != MediaStatus.REMOVING)
            throw new IllegalArgumentException("Only downloaded media has a path");
    }

    /** @return a download due now when the content carries media, or null when there is nothing to download */
    public static MediaDownload of(MessageContent content, Instant now) {
        return content.mediaId() == null ? null : new MediaDownload(MediaStatus.PENDING, null, 0, now, null);
    }

    /**
     * Meta's size limits per kind of media, checked against Meta's docs on 2026-10-22: images 5 MB, audio and video
     * 16 MB, documents 100 MB, stickers 500 KB.
     *
     * @return the largest file Takodu downloads for the kind
     */
    public static long maxBytes(MessageKind kind) {
        return switch (kind) {
            case IMAGE -> 5 * MB;
            case AUDIO, VIDEO -> 16 * MB;
            case STICKER -> MB / 2;
            default -> 100 * MB;
        };
    }

    public MediaDownload stored(String storedPath) {
        if (status != MediaStatus.PENDING) throw new IllegalStateException("Only pending media is downloaded");
        return new MediaDownload(MediaStatus.READY, Objects.requireNonNull(storedPath, "path is required"),
                attempts + 1, null, null);
    }

    /**
     * A failed download waits and is tried again, doubling the wait, until the fifth attempt. A failure that will
     * repeat, such as a file over the limit, ends it at once.
     *
     * @param retryable whether trying again may succeed, as after a timeout or an expired url
     */
    public MediaDownload failed(String why, boolean retryable, Instant now) {
        if (status != MediaStatus.PENDING) throw new IllegalStateException("Only pending media is downloaded");
        var tried = attempts + 1;
        if (!retryable || tried >= MAX_ATTEMPTS) return new MediaDownload(MediaStatus.FAILED, null, tried, null, why);
        return new MediaDownload(MediaStatus.PENDING, null, tried, now.plus(FIRST_RETRY.multipliedBy(1L << (tried - 1))),
                why);
    }

    /** C5: the customer was deleted, so the file goes too; the retention job deletes it. */
    public MediaDownload removalRequested() {
        if (status.isRemoved()) return this;
        return new MediaDownload(MediaStatus.REMOVING, path, attempts, null, null);
    }

    /** The file was deleted, or never downloaded; the message keeps its kind and caption. */
    public MediaDownload removed() {
        return new MediaDownload(MediaStatus.REMOVED, null, attempts, null, null);
    }
}
