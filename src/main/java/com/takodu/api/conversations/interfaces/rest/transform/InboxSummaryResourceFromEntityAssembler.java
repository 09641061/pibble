package com.takodu.api.conversations.interfaces.rest.transform;

import com.takodu.api.conversations.domain.model.valueobjects.InboxSummary;
import com.takodu.api.conversations.interfaces.rest.resources.InboxSummaryResource;

public final class InboxSummaryResourceFromEntityAssembler {
    private InboxSummaryResourceFromEntityAssembler() { }

    public static InboxSummaryResource toResourceFromEntity(InboxSummary summary) {
        return new InboxSummaryResource(summary.awaitingReply(), summary.unread());
    }
}
