package com.takodu.api.customers.domain.repositories;

import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    /** @return the customer unless deleted */
    Optional<Customer> findActiveById(OrganizationId organizationId, CustomerId customerId);

    Optional<Customer> findActiveByPhone(OrganizationId organizationId, PhoneNumber phone);

    /**
     * @param text matched, ignoring case, inside the name and the WhatsApp name, and its digits inside the phone;
     *        blank matches every active customer
     * @return active customers by display name
     */
    List<Customer> search(OrganizationId organizationId, String text, int offset, int limit);

    Optional<Customer> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey);

    /**
     * @throws com.takodu.api.customers.domain.exceptions.CustomerAlreadyRegisteredException when another active
     *         customer has the phone (C1)
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    Customer save(Customer customer);
}
