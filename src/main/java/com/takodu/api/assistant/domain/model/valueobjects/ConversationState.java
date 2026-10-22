package com.takodu.api.assistant.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;

/**
 * The chat as the assistant sees it before answering (A2).
 *
 * @param windowOpen whether free-form messages may be sent now (W6)
 * @param paused whether a person wrote in the last 12 hours or the chat was handed off (A3, A7)
 */
public record ConversationState(ConversationId conversationId, EstablishmentId establishmentId, CustomerId customerId,
                                boolean lineConnected, boolean windowOpen, boolean paused) { }
