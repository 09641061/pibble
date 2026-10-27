package com.takodu.api.conversations.application.queryservices;

import com.takodu.api.conversations.domain.model.events.InboxEvent;
import com.takodu.api.conversations.domain.model.queries.StreamInboxChangesQuery;
import io.smallrye.mutiny.Multi;

public interface InboxStreamQueryService {

    /** @return the inbox events of the member's organization and establishments, as they commit */
    Multi<InboxEvent> handle(StreamInboxChangesQuery query);
}
