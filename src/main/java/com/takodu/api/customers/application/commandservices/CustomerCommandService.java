package com.takodu.api.customers.application.commandservices;

import com.takodu.api.customers.domain.exceptions.CustomerAlreadyRegisteredException;
import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.commands.RegisterCustomerCommand;

/** Customers are not settings, so every method works while the plan is inactive (O6). */
public interface CustomerCommandService {

    /**
     * @return the new customer, or the one an earlier request with the same key created
     * @throws CustomerAlreadyRegisteredException when an active customer has the phone (C1)
     */
    Customer handle(RegisterCustomerCommand command);
}
