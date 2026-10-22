package com.takodu.api.conversations.application.queryservices;

import com.takodu.api.conversations.domain.model.events.ConversationChangedEvent;
import com.takodu.api.conversations.domain.model.queries.StreamInboxChangesQuery;
import io.smallrye.mutiny.Multi;

public interface InboxStreamQueryService {

    /** @return the inbox changes of the member's organization and establishments, as they commit */
    Multi<ConversationChangedEvent> handle(StreamInboxChangesQuery query);
}
