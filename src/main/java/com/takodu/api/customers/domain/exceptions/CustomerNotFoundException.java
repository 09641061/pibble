package com.takodu.api.customers.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;

public class CustomerNotFoundException extends ResourceNotFoundException {
    public CustomerNotFoundException(CustomerId customerId) { super("Customer", customerId.value()); }
}
