package com.takodu.api.booking.application.commandservices;

import com.takodu.api.booking.domain.exceptions.AppointmentChangedException;
import com.takodu.api.booking.domain.exceptions.AppointmentNotFoundException;
import com.takodu.api.booking.domain.exceptions.CustomerNotFoundException;
import com.takodu.api.booking.domain.exceptions.InvalidAppointmentTransitionException;
import com.takodu.api.booking.domain.exceptions.MemberCannotPerformServiceException;
import com.takodu.api.booking.domain.exceptions.NoShowBeforeStartException;
import com.takodu.api.booking.domain.exceptions.OutsideBookingWindowException;
import com.takodu.api.booking.domain.exceptions.OutsideOpeningHoursException;
import com.takodu.api.booking.domain.exceptions.ServiceNotBookableException;
import com.takodu.api.booking.domain.exceptions.ServiceNotFoundException;
import com.takodu.api.booking.domain.exceptions.SlotUnavailableException;
import com.takodu.api.booking.domain.exceptions.TimeBlockedException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.commands.BookAppointmentCommand;
import com.takodu.api.booking.domain.model.commands.CancelAppointmentCommand;
import com.takodu.api.booking.domain.model.commands.ChangeAppointmentStatusCommand;
import com.takodu.api.booking.domain.model.commands.RescheduleAppointmentCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;

/**
 * Transitions take an idempotency key: a retry with the key of the last applied transition returns the current
 * state. Every method throws {@link AppointmentNotFoundException} when the appointment is not in the organization,
 * {@link AccessDeniedException} when the caller is staff not assigned to its establishment, and
 * {@link AppointmentChangedException} when another request changed it at the same time.
 */
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

    /**
     * B13: re-runs B2-B6 and B9 on the new range and publishes {@code AppointmentRescheduledIntegrationEvent}.
     *
     * @throws PlanInactiveException B7
     * @throws InvalidAppointmentTransitionException when the appointment is not confirmed
     * @throws MemberCannotPerformServiceException B2
     * @throws OutsideBookingWindowException B9
     * @throws OutsideOpeningHoursException B3
     * @throws TimeBlockedException B4
     * @throws SlotUnavailableException B5, B6
     */
    Appointment handle(RescheduleAppointmentCommand command);

    /**
     * B12: works while the plan is inactive, and publishes {@code AppointmentCancelledIntegrationEvent}.
     *
     * @throws InvalidAppointmentTransitionException when the appointment is already final (B10)
     */
    Appointment handle(CancelAppointmentCommand command);

    /**
     * B10, B11: works while the plan is inactive.
     *
     * @throws InvalidAppointmentTransitionException when B10 does not allow the move
     * @throws NoShowBeforeStartException B11
     */
    Appointment handle(ChangeAppointmentStatusCommand command);
}
