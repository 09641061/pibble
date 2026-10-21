package com.takodu.api.booking.application.queryservices;

import com.takodu.api.booking.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.queries.GetAppointmentByIdQuery;
import com.takodu.api.booking.domain.model.queries.GetAppointmentsByEstablishmentQuery;
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
}
