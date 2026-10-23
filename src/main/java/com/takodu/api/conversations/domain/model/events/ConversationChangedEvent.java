package com.takodu.api.conversations.domain.model.events;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * Something the inbox shows changed: a message arrived or was sent, a status moved, the chat was read or assigned.
 * Fired inside the transaction; the inbox stream forwards it after commit. Not an integration event.
 */
public record ConversationChangedEvent(OrganizationId organizationId, EstablishmentId establishmentId,
                                       ConversationId conversationId) { }
