package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** B1: the service is inactive, its establishment is archived, or nobody performs it. */
public class ServiceNotBookableException extends BusinessRuleViolationException {
    public ServiceNotBookableException(String message) { super("SERVICE_NOT_BOOKABLE", message); }
}
