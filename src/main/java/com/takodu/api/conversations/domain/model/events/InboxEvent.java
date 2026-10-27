package com.takodu.api.conversations.domain.model.events;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * What the inbox stream forwards: a conversation changed, the assistant started or stopped answering one, or an
 * appointment changed.
 */
public sealed interface InboxEvent permits ConversationChangedEvent, AssistantActivityEvent, AppointmentChangedEvent {
    OrganizationId organizationId();

    EstablishmentId establishmentId();
}
