package com.takodu.api.booking.application.queryservices;

import com.takodu.api.booking.domain.exceptions.CustomerNotFoundException;
import com.takodu.api.booking.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.booking.domain.exceptions.ServiceNotBookableException;
import com.takodu.api.booking.domain.exceptions.ServiceNotFoundException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.queries.GetAppointmentByIdQuery;
import com.takodu.api.booking.domain.model.queries.GetAppointmentsByEstablishmentQuery;
import com.takodu.api.booking.domain.model.queries.GetAvailableSlotsQuery;
import com.takodu.api.booking.domain.model.valueobjects.Slot;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;

import java.util.List;
import java.util.Optional;

public interface AppointmentQueryService {
    /** @throws AccessDeniedException when the caller is staff not assigned to the appointment's establishment */
    Optional<Appointment> handle(GetAppointmentByIdQuery query);

    /**
     * @return the calendar, earliest first; it shows appointments outside today's opening hours too (O8)
     * @throws EstablishmentNotFoundException when the establishment is not in the organization
     * @throws AccessDeniedException when the caller is staff not assigned to it
     */
    List<Appointment> handle(GetAppointmentsByEstablishmentQuery query);

    /**
     * B16: free slots for staff booking; they ignore the plan, which only booking itself checks (B7).
     *
     * @return slots earliest first
     * @throws ServiceNotFoundException when the service is not in the organization
     * @throws AccessDeniedException when the caller is staff not assigned to the service's establishment
     * @throws ServiceNotBookableException when the service is inactive or its establishment archived (B1)
     * @throws CustomerNotFoundException when the customer is deleted or not in the organization
     */
    List<Slot> handle(GetAvailableSlotsQuery query);
}
