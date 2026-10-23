package com.takodu.api.organization.interfaces.acl;

import java.time.Instant;
import java.util.UUID;

/**
 * A question {@code organization} asks a context above it: implemented by {@code booking}, which owns appointments,
 * so {@code organization} never depends on {@code booking}
 * (docs/decisions/implemented/2026-10-21-lower-contexts-ask-up-through-providers.md).
 */
public interface UpcomingAppointmentsProvider {

    /**
     * @return whether the member has appointments that have not started yet and are not cancelled
     */
    boolean hasUpcomingAppointments(UUID organizationId, UUID memberId, Instant now);
}
