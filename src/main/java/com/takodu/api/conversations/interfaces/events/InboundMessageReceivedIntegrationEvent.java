package com.takodu.api.conversations.interfaces.events;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer message was stored for the first time; the assistant decides from it whether to answer (A2). Not
 * published for echoes, history or redeliveries.
 *
 * @param kind the message kind in lower case, such as {@code text} or {@code audio}
 */
public record InboundMessageReceivedIntegrationEvent(UUID organizationId, UUID messageId, UUID conversationId,
                                                     UUID establishmentId, UUID customerId, String kind,
                                                     Instant occurredAt) { }
