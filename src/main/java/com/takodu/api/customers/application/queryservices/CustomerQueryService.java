package com.takodu.api.customers.application.queryservices;

import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.queries.GetCustomerByIdQuery;

import java.util.Optional;

/** Every member sees every customer of the organization, whatever their establishments. */
public interface CustomerQueryService {
    /** @return the customer, empty when deleted or in another organization */
    Optional<Customer> handle(GetCustomerByIdQuery query);
}
