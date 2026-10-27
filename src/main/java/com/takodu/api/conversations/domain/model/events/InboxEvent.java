package com.takodu.api.conversations.domain.model.events;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** What the inbox stream forwards: a conversation changed, or the assistant started or stopped answering one. */
public sealed interface InboxEvent permits ConversationChangedEvent, AssistantActivityEvent {
    OrganizationId organizationId();

    EstablishmentId establishmentId();

    ConversationId conversationId();
}
