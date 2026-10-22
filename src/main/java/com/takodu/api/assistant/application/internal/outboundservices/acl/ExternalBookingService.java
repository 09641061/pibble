package com.takodu.api.assistant.application.internal.outboundservices.acl;

import com.takodu.api.assistant.domain.model.valueobjects.AppointmentId;
import com.takodu.api.assistant.domain.model.valueobjects.CustomerAppointment;
import com.takodu.api.booking.interfaces.acl.AppointmentView;
import com.takodu.api.booking.interfaces.acl.BookingContextFacade;
import com.takodu.api.booking.interfaces.acl.SlotView;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The only class in assistant that talks to booking. Booking checks the bound assistant and applies B9 and B14;
 * its refusals arrive as the shared kernel's domain exceptions, which the tools turn into text.
 */
@ApplicationScoped
public class ExternalBookingService {
    private final BookingContextFacade bookingContextFacade;

    public ExternalBookingService(BookingContextFacade bookingContextFacade) {
        this.bookingContextFacade = bookingContextFacade;
    }

    /** @param member only this member's times; null for anyone who performs the service */
    public List<Instant> fetchFreeTimes(OrganizationId organizationId, ServiceId serviceId, CustomerId customerId,
                                        LocalDate date, MemberId member) {
        return bookingContextFacade.fetchSlots(organizationId.value(), serviceId.value(), customerId.value(), date,
                member == null ? null : member.value()).stream().map(SlotView::start).toList();
    }

    /** @param member the member the customer asked for; null lets booking choose (B17) */
    public CustomerAppointment book(OrganizationId organizationId, CustomerId customerId, ServiceId serviceId,
                                    MemberId member, Instant start, UUID idempotencyKey) {
        return appointment(bookingContextFacade.book(organizationId.value(), customerId.value(), serviceId.value(),
                member == null ? null : member.value(), start, idempotencyKey));
    }

    public List<CustomerAppointment> fetchUpcoming(OrganizationId organizationId, EstablishmentId establishmentId,
                                                   CustomerId customerId) {
        return bookingContextFacade.fetchUpcomingAppointments(organizationId.value(), establishmentId.value(),
                customerId.value()).stream().map(ExternalBookingService::appointment).toList();
    }

    public CustomerAppointment reschedule(OrganizationId organizationId, CustomerId customerId,
                                          AppointmentId appointmentId, Instant start, UUID idempotencyKey) {
        return appointment(bookingContextFacade.reschedule(organizationId.value(), customerId.value(),
                appointmentId.value(), start, idempotencyKey));
    }

    public CustomerAppointment cancel(OrganizationId organizationId, CustomerId customerId,
                                      AppointmentId appointmentId, String reason, UUID idempotencyKey) {
        return appointment(bookingContextFacade.cancel(organizationId.value(), customerId.value(),
                appointmentId.value(), reason, idempotencyKey));
    }

    private static CustomerAppointment appointment(AppointmentView view) {
        return new CustomerAppointment(new AppointmentId(view.id()), new ServiceId(view.serviceId()),
                new MemberId(view.staffMemberId()), view.start(), view.customerMayChange());
    }
}
