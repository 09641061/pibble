package com.takodu.api.booking.domain.model.queries;

import com.takodu.api.booking.domain.model.valueobjects.BookingSource;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.LocalDate;

/**
 * B16
 *
 * @param date a day in the establishment's time zone
 * @param staffMemberId only this member's slots; null for every performer
 * @param customerId leaves out the times this customer is busy; null when nobody is booking yet
 * @param source who will book: the assistant sees only what B9 lets it book
 */
public record GetAvailableSlotsQuery(OrganizationId organizationId, ServiceId serviceId, LocalDate date,
                                     MemberId staffMemberId, CustomerId customerId, BookingSource source) {
    public GetAvailableSlotsQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (serviceId == null) throw new IllegalArgumentException("serviceId is required");
        if (date == null) throw new IllegalArgumentException("date is required");
        if (source == null) throw new IllegalArgumentException("source is required");
    }
}
