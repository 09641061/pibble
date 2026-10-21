package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;

/** The customer is deleted or not in the organization; deleted customers get no new appointments (C5). */
public class CustomerNotFoundException extends ResourceNotFoundException {
    public CustomerNotFoundException(CustomerId customerId) { super("Customer", customerId.value()); }
}
