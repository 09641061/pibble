package com.takodu.api.booking.interfaces.acl;

import java.time.Instant;
import java.util.UUID;

/**
 * An appointment as other contexts see it.
 *
 * @param status lower case, such as {@code confirmed} or {@code cancelled}
 * @param customerMayChange whether the customer may still reschedule or cancel it through the assistant (B14)
 */
public record AppointmentView(UUID id, UUID establishmentId, UUID serviceId, UUID staffMemberId, Instant start,
                              Instant end, String status, boolean customerMayChange) { }
