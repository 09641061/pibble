package com.takodu.api.customers.domain.repositories;

import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.Optional;

public interface CustomerRepository {
    /** @return the customer unless deleted */
    Optional<Customer> findActiveById(OrganizationId organizationId, CustomerId customerId);

    Optional<Customer> findActiveByPhone(OrganizationId organizationId, PhoneNumber phone);

    Optional<Customer> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey);

    /**
     * @throws com.takodu.api.customers.domain.exceptions.CustomerAlreadyRegisteredException when another active
     *         customer has the phone (C1)
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    Customer save(Customer customer);
}
