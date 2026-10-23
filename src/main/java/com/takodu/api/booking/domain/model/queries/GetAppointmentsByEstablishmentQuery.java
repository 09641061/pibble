package com.takodu.api.booking.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;

import java.time.Duration;

/**
 * The calendar: every appointment, in any status, overlapping the window.
 *
 * @param window at most {@link #MAX_WINDOW} long
 */
public record GetAppointmentsByEstablishmentQuery(OrganizationId organizationId, EstablishmentId establishmentId,
                                                  TimeRange window) {
    public static final Duration MAX_WINDOW = Duration.ofDays(42);

    public GetAppointmentsByEstablishmentQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (window == null) throw new IllegalArgumentException("from and to are required");
        if (window.length().compareTo(MAX_WINDOW) > 0) throw new IllegalArgumentException("Ask for at most 42 days");
    }
}
