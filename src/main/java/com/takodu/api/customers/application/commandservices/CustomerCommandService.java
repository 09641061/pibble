package com.takodu.api.customers.application.commandservices;

import com.takodu.api.customers.domain.exceptions.CustomerAlreadyRegisteredException;
import com.takodu.api.customers.domain.exceptions.CustomerNotFoundException;
import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.commands.DeleteCustomerCommand;
import com.takodu.api.customers.domain.model.commands.RegisterCustomerCommand;
import com.takodu.api.customers.domain.model.commands.UpdateCustomerCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;

/** Customers are not settings, so every method works while the plan is inactive (O6). */
public interface CustomerCommandService {

    /**
     * @return the new customer, or the one an earlier request with the same key created
     * @throws CustomerAlreadyRegisteredException when an active customer has the phone (C1)
     */
    Customer handle(RegisterCustomerCommand command);

    /** @throws CustomerNotFoundException when the customer is deleted or not in the organization */
    Customer handle(UpdateCustomerCommand command);

    /**
     * @throws AccessDeniedException when the caller is staff
     * @throws CustomerNotFoundException when the customer is deleted or not in the organization
     */
    void handle(DeleteCustomerCommand command);
}
