package com.takodu.api.booking.application.internal.outboundservices.acl;

import com.takodu.api.booking.application.queryservices.AppointmentQueryService;
import com.takodu.api.booking.domain.model.queries.GetUpcomingAppointmentsByMemberQuery;
import com.takodu.api.organization.interfaces.acl.UpcomingAppointmentsProvider;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.UUID;

/** Answers organization's question before it removes a member (O12): booking owns the appointments. */
@ApplicationScoped
public class BookingUpcomingAppointmentsProvider implements UpcomingAppointmentsProvider {
    private final AppointmentQueryService appointmentQueryService;

    public BookingUpcomingAppointmentsProvider(AppointmentQueryService appointmentQueryService) {
        this.appointmentQueryService = appointmentQueryService;
    }

    @Override
    public boolean hasUpcomingAppointments(UUID organizationId, UUID memberId, Instant now) {
        return !appointmentQueryService.handle(new GetUpcomingAppointmentsByMemberQuery(new OrganizationId(organizationId),
                new MemberId(memberId), now)).isEmpty();
    }
}
