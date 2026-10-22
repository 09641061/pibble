package com.takodu.api.conversations.interfaces.acl;

import java.util.UUID;

/**
 * A chat as the assistant sees it before answering (A2).
 *
 * @param windowOpen whether free-form messages may be sent now (W6)
 * @param assistantPaused whether a person wrote in the last 12 hours or the chat was handed off (A3, A7)
 */
public record AssistantConversationView(UUID conversationId, UUID establishmentId, UUID customerId,
                                        boolean lineConnected, boolean windowOpen, boolean assistantPaused) { }
