package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** C5: the chat of a deleted customer stays readable, but nothing new is sent to them. */
public class CustomerDeletedException extends BusinessRuleViolationException {
    public CustomerDeletedException() {
        super("CUSTOMER_DELETED", "The customer was deleted; nothing more is sent to them");
    }
}
