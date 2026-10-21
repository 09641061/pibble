package com.takodu.api.booking.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.time.Instant;

/**
 * The member's confirmed appointments that start after {@code now}, which keep them from being removed (O12).
 * Answered without checking the caller.
 */
public record GetUpcomingAppointmentsByMemberQuery(OrganizationId organizationId, MemberId memberId, Instant now) {
    public GetUpcomingAppointmentsByMemberQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (memberId == null) throw new IllegalArgumentException("memberId is required");
        if (now == null) throw new IllegalArgumentException("now is required");
    }
}
