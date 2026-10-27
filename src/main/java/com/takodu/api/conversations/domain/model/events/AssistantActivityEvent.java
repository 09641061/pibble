package com.takodu.api.conversations.domain.model.events;

import com.takodu.api.conversations.domain.model.valueobjects.ConversationId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * The assistant started answering a conversation, or finished whatever the outcome, so the inbox can show it typing.
 * Fired outside any transaction, while the turn runs. Not an integration event.
 *
 * @param typing true when a turn started running, false when it ended
 */
public record AssistantActivityEvent(OrganizationId organizationId, EstablishmentId establishmentId,
                                     ConversationId conversationId, boolean typing) implements InboxEvent { }
