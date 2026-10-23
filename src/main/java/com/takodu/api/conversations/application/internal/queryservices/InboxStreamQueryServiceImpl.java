package com.takodu.api.conversations.application.internal.queryservices;

import com.takodu.api.conversations.application.internal.eventhandlers.ConversationChangedEventHandler;
import com.takodu.api.conversations.application.queryservices.InboxStreamQueryService;
import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import com.takodu.api.conversations.domain.model.queries.StreamInboxChangesQuery;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InboxStreamQueryServiceImpl implements InboxStreamQueryService {
    private final ConversationChangedEventHandler conversationChangedEventHandler;
    private final AccessPolicy accessPolicy;

    public InboxStreamQueryServiceImpl(ConversationChangedEventHandler conversationChangedEventHandler,
                                       AccessPolicy accessPolicy) {
        this.conversationChangedEventHandler = conversationChangedEventHandler;
        this.accessPolicy = accessPolicy;
    }

    /**
     * The filter runs on the publisher's thread, after the request, so it uses only the query's values. A slow
     * client gets a buffer instead of failing the broadcast.
     */
    @Override
    public Multi<ConversationChangedEvent> handle(StreamInboxChangesQuery query) {
        accessPolicy.require(Action.REPLY_IN_INBOX);
        return conversationChangedEventHandler.changes()
                .filter(change -> change.organizationId().equals(query.organizationId())
                        && query.sees(change.establishmentId()))
                .onOverflow().buffer(256);
    }
}
