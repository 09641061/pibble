package com.takodu.api.conversations.interfaces.rest.resources;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * @param customerName the typed name, else the WhatsApp profile name, else the phone
 * @param windowClosesAt when free-form replies stop (W6); null when the customer never wrote
 * @param assistantPausedUntil until when the assistant stays quiet after a person wrote (A3)
 * @param assistantHandedOff whether the assistant handed the chat to a person and stays quiet until resumed (A7)
 * @param handoffSummary why the assistant handed it off, for the person who takes it
 */
@Schema(name = "Conversation")
public record ConversationResource(UUID id, UUID establishmentId, UUID customerId, String customerName,
                                   String customerPhone, boolean customerDeleted, UUID assignedMemberId,
                                   int unreadCount, Instant lastMessageAt, Instant lastInboundAt, boolean windowOpen,
                                   Instant windowClosesAt, boolean assistantPaused, Instant assistantPausedUntil,
                                   boolean assistantHandedOff, String handoffSummary) { }
