package com.takodu.api.conversations;

import com.takodu.api.conversations.interfaces.events.InboundMessageReceivedIntegrationEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Stands in for the assistant's listener: records conversations' integration events in every test. */
@Singleton
public class RecordingConversationEvents {
    private final List<InboundMessageReceivedIntegrationEvent> events = new CopyOnWriteArrayList<>();

    void on(@Observes InboundMessageReceivedIntegrationEvent event) {
        events.add(event);
    }

    /** @return the events published about the conversation, oldest first */
    public List<InboundMessageReceivedIntegrationEvent> about(UUID conversationId) {
        return events.stream().filter(event -> event.conversationId().equals(conversationId)).toList();
    }
}
