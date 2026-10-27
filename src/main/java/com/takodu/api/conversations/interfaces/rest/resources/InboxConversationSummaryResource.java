package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

/**
 * What the inbox list shows of a conversation, with the same meaning as the fields of {@code Conversation}.
 *
 * @param windowClosesAt when free-form replies stop (W6); null when the customer never wrote
 * @param assistantPausedUntil until when the assistant stays quiet after a person wrote (A3)
 * @param assistantHandedOff whether the assistant handed the chat to a person and stays quiet until resumed (A7)
 * @param assistantPausedManually whether a member paused the assistant until someone resumes it (A3)
 */
@Schema(name = "InboxConversationSummary")
public record InboxConversationSummaryResource(Instant lastMessageAt, Instant lastInboundAt, int unreadCount,
                                               Instant windowClosesAt, Instant assistantPausedUntil,
                                               boolean assistantHandedOff, boolean assistantPausedManually) { }
