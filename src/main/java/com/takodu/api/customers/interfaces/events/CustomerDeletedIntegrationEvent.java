package com.takodu.api.customers.interfaces.events;

import java.util.UUID;

/** C5: a customer was deleted; conversations deletes the media they sent. */
public record CustomerDeletedIntegrationEvent(UUID organizationId, UUID customerId) { }
