package com.takodu.api.conversations.domain.model.queries;

import com.takodu.api.conversations.domain.model.valueobjects.MessageId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** The downloaded file of a message's image, audio, video, document or sticker. */
public record GetMessageMediaQuery(OrganizationId organizationId, MessageId messageId) {
    public GetMessageMediaQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (messageId == null) throw new IllegalArgumentException("messageId is required");
    }
}
