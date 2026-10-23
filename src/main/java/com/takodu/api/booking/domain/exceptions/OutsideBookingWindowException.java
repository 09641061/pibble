package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** B9: how far in the past staff and how soon or far ahead the assistant may book. */
public class OutsideBookingWindowException extends BusinessRuleViolationException {
    public OutsideBookingWindowException(String message) { super("OUTSIDE_BOOKING_WINDOW", message); }
}
