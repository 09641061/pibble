package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** B4: a time block of the member or of the whole establishment overlaps. */
public class TimeBlockedException extends BusinessRuleViolationException {
    public TimeBlockedException() { super("TIME_BLOCKED", "The member or the establishment is blocked then"); }
}
