package com.takodu.api.conversations.application.internal.eventhandlers;

import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.operators.multi.processors.SerializedProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

/**
 * Forwards committed inbox changes to every open stream of this instance. Only an optimization: an app that missed
 * a change, or reconnects, reloads the inbox.
 */
@ApplicationScoped
public class ConversationChangedEventHandler {
    /** Serialized, because commits on several threads publish at once. */
    private final SerializedProcessor<ConversationChangedEvent, ConversationChangedEvent> changes =
            BroadcastProcessor.<ConversationChangedEvent>create().serialized();

    void on(@Observes(during = TransactionPhase.AFTER_SUCCESS) ConversationChangedEvent event) {
        changes.onNext(event);
    }

    /** @return the changes committed from now on, until the subscriber cancels */
    public Multi<ConversationChangedEvent> changes() {
        return Multi.createFrom().publisher(changes);
    }
}
