package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** B10, B13: the appointment's status does not allow the change. */
public class InvalidAppointmentTransitionException extends BusinessRuleViolationException {
    public InvalidAppointmentTransitionException(String message) { super("INVALID_TRANSITION", message); }
}
