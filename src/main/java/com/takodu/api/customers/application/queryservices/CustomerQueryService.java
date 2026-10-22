package com.takodu.api.customers.application.queryservices;

import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.queries.GetCustomerByIdQuery;
import com.takodu.api.customers.domain.model.queries.GetCustomerIncludingDeletedQuery;
import com.takodu.api.customers.domain.model.queries.GetCustomersIncludingDeletedQuery;
import com.takodu.api.customers.domain.model.queries.SearchCustomersQuery;

import java.util.List;
import java.util.Optional;

/** Every member sees every customer of the organization, whatever their establishments. */
public interface CustomerQueryService {
    /** @return the customer, empty when deleted or in another organization */
    Optional<Customer> handle(GetCustomerByIdQuery query);

    /** @return active customers by display name; digits in the text also match the phone */
    CustomerPage handle(SearchCustomersQuery query);

    /** @return the customer even when deleted, so history keeps its name (C5); empty in another organization */
    Optional<Customer> handle(GetCustomerIncludingDeletedQuery query);

    /** @return the customers found, deleted or not, in no particular order; ids of other organizations are skipped */
    List<Customer> handle(GetCustomersIncludingDeletedQuery query);
}
