package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link WhatsappGateway} of every {@code @QuarkusTest}: records sends per recipient and fails on demand. Keyed
 * by recipient, because the sender job also delivers messages other tests left pending.
 */
@Singleton
public class RecordingWhatsappGateway implements WhatsappGateway {
    private final Map<String, List<String>> sent = new ConcurrentHashMap<>();
    private final Map<String, Failure> failures = new ConcurrentHashMap<>();

    @Override
    public Wamid sendText(WhatsappLine line, String to, String body) {
        return record(to, body);
    }

    /** Records the template as {@code name(parameter|parameter|...)}, beside the texts. */
    @Override
    public Wamid sendTemplate(WhatsappLine line, String to, String templateName, String language,
                              List<String> parameters) {
        return record(to, templateName + "(" + String.join("|", parameters) + ")");
    }

    private Wamid record(String to, String sentContent) {
        var failure = failures.get(to);
        if (failure != null && failure.remaining.getAndDecrement() > 0)
            throw new WhatsappSendException(failure.error, failure.retryable);
        sent.computeIfAbsent(to, recipient -> new CopyOnWriteArrayList<>()).add(sentContent);
        return new Wamid("wamid.SENT" + UUID.randomUUID().toString().replace("-", ""));
    }

    /** @return the texts and templates sent to the WhatsApp id, oldest first */
    public List<String> sentTo(String waId) {
        return sent.getOrDefault(waId, List.of());
    }

    /** Makes the next {@code times} sends to the recipient fail as Meta would. */
    public void failFor(String waId, int times, String error, boolean retryable) {
        failures.put(waId, new Failure(new AtomicInteger(times), error, retryable));
    }

    private record Failure(AtomicInteger remaining, String error, boolean retryable) { }
}
