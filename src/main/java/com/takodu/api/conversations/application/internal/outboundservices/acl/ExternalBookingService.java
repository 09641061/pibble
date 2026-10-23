package com.takodu.api.conversations.application.internal.outboundservices.acl;

import com.takodu.api.booking.interfaces.acl.BookingContextFacade;
import com.takodu.api.conversations.domain.model.valueobjects.AppointmentId;
import com.takodu.api.conversations.domain.model.valueobjects.UpcomingAppointment;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * The only class in conversations that calls booking. Notifications still learn about appointments from booking's
 * events; staff templates ask for the appointment's current state.
 */
@ApplicationScoped
public class ExternalBookingService {
    private final BookingContextFacade bookingContextFacade;

    public ExternalBookingService(BookingContextFacade bookingContextFacade) {
        this.bookingContextFacade = bookingContextFacade;
    }

    /** @return the appointment when it is one of the customer's upcoming ones at the establishment; empty otherwise */
    public Optional<UpcomingAppointment> fetchUpcomingAppointment(OrganizationId organizationId,
                                                                  EstablishmentId establishmentId,
                                                                  CustomerId customerId, AppointmentId appointmentId) {
        return bookingContextFacade.fetchUpcomingAppointments(organizationId.value(), establishmentId.value(),
                        customerId.value()).stream()
                .filter(view -> view.id().equals(appointmentId.value()))
                .findFirst()
                .map(view -> new UpcomingAppointment(new AppointmentId(view.id()), new ServiceId(view.serviceId()),
                        view.start()));
    }
}
