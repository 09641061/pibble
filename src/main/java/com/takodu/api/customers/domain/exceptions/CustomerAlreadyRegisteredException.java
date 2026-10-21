package com.takodu.api.customers.domain.exceptions;

import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.shared.domain.exceptions.ConflictException;

/** C1: a phone belongs to at most one active customer of an organization. */
public class CustomerAlreadyRegisteredException extends ConflictException {
    public CustomerAlreadyRegisteredException(PhoneNumber phone) {
        super("Customer", "A customer with this phone already exists: " + phone.e164());
    }
}
