package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import jakarta.inject.Singleton;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link WhatsappMediaGateway} of every {@code @QuarkusTest}: serves the bytes a test put under a media id and
 * fails on demand. An unknown id fails as Meta would, so media other tests left pending never downloads by accident.
 */
@Singleton
public class RecordingWhatsappMediaGateway implements WhatsappMediaGateway {
    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> downloads = new ConcurrentHashMap<>();
    private final Map<String, Failure> failures = new ConcurrentHashMap<>();

    @Override
    public InputStream download(WhatsappLine line, String mediaId, long maxBytes) {
        downloads.computeIfAbsent(mediaId, id -> new AtomicInteger()).incrementAndGet();
        var failure = failures.get(mediaId);
        if (failure != null && failure.remaining.getAndDecrement() > 0)
            throw new WhatsappMediaException(failure.error, failure.retryable);
        var file = files.get(mediaId);
        if (file == null) throw new WhatsappMediaException("Meta refused the media: HTTP 404", false);
        if (file.length > maxBytes) throw new WhatsappMediaException("The file is larger than WhatsApp allows", false);
        return new ByteArrayInputStream(file);
    }

    /** Meta has this file under the id. */
    public void serve(String mediaId, byte[] content) {
        files.put(mediaId, content);
    }

    /** Makes the next {@code times} downloads of the id fail as Meta would. */
    public void failFor(String mediaId, int times, String error, boolean retryable) {
        failures.put(mediaId, new Failure(new AtomicInteger(times), error, retryable));
    }

    public int downloadsOf(String mediaId) {
        var count = downloads.get(mediaId);
        return count == null ? 0 : count.get();
    }

    private record Failure(AtomicInteger remaining, String error, boolean retryable) { }
}
