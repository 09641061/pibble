package com.takodu.api.conversations.application.queryservices;

import com.takodu.api.conversations.domain.model.aggregates.Conversation;

/**
 * A conversation with what the assistant needs to decide whether it may answer (A2).
 *
 * @param lineConnected whether the line can still send (W15)
 */
public record AssistantConversation(Conversation conversation, boolean lineConnected) { }
