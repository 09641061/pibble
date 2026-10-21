package com.takodu.api.booking.application.commandservices;

import com.takodu.api.booking.domain.exceptions.CustomerNotFoundException;
import com.takodu.api.booking.domain.exceptions.MemberCannotPerformServiceException;
import com.takodu.api.booking.domain.exceptions.OutsideBookingWindowException;
import com.takodu.api.booking.domain.exceptions.OutsideOpeningHoursException;
import com.takodu.api.booking.domain.exceptions.ServiceNotBookableException;
import com.takodu.api.booking.domain.exceptions.ServiceNotFoundException;
import com.takodu.api.booking.domain.exceptions.SlotUnavailableException;
import com.takodu.api.booking.domain.exceptions.TimeBlockedException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.commands.BookAppointmentCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;

public interface AppointmentCommandService {

    /**
     * B1-B9: books the service with the chosen member, or with the one B17 picks, and publishes
     * {@code AppointmentBookedIntegrationEvent}.
     *
     * @return the new appointment, or the one an earlier request with the same key booked
     * @throws ServiceNotFoundException when the service is not in the organization
     * @throws AccessDeniedException when the caller is staff not assigned to the service's establishment
     * @throws PlanInactiveException B7
     * @throws ServiceNotBookableException B1
     * @throws CustomerNotFoundException when the customer is deleted or not in the organization
     * @throws MemberCannotPerformServiceException B2
     * @throws OutsideBookingWindowException B9
     * @throws OutsideOpeningHoursException B3
     * @throws TimeBlockedException B4
     * @throws SlotUnavailableException B5, B6, also when a concurrent booking took the time first
     */
    Appointment handle(BookAppointmentCommand command);
}
