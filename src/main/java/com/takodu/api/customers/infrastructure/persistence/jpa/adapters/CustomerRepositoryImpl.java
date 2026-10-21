package com.takodu.api.customers.infrastructure.persistence.jpa.adapters;

import com.takodu.api.customers.domain.exceptions.CustomerAlreadyRegisteredException;
import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.customers.domain.repositories.CustomerRepository;
import com.takodu.api.customers.infrastructure.persistence.jpa.assemblers.CustomerPersistenceAssembler;
import com.takodu.api.customers.infrastructure.persistence.jpa.repositories.CustomerPersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Optional;

@ApplicationScoped
public class CustomerRepositoryImpl implements CustomerRepository {
    private static final String PHONE_UNIQUE = "customers_phone_uk";
    private static final String IDEMPOTENCY_UNIQUE = "customers_idempotency_uk";

    private final CustomerPersistenceRepository customerPersistenceRepository;

    public CustomerRepositoryImpl(CustomerPersistenceRepository customerPersistenceRepository) {
        this.customerPersistenceRepository = customerPersistenceRepository;
    }

    @Override
    public Optional<Customer> findActiveById(OrganizationId organizationId, CustomerId customerId) {
        return customerPersistenceRepository.findActiveById(organizationId.value(), customerId.value())
                .map(CustomerPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Customer> findActiveByPhone(OrganizationId organizationId, PhoneNumber phone) {
        return customerPersistenceRepository.findActiveByPhone(organizationId.value(), phone.e164())
                .map(CustomerPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Customer> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey) {
        return customerPersistenceRepository.findByIdempotencyKey(organizationId.value(), idempotencyKey.value())
                .map(CustomerPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Customer save(Customer customer) {
        var entity = CustomerPersistenceAssembler.toPersistenceFromDomain(customer);
        try {
            if (entity.getId() == null) {
                customerPersistenceRepository.persist(entity);
            } else {
                entity = customerPersistenceRepository.getEntityManager().merge(entity);
            }
            customerPersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (e.getConstraintName()) {
                case PHONE_UNIQUE -> throw new CustomerAlreadyRegisteredException(customer.getPhone());
                case IDEMPOTENCY_UNIQUE -> throw new RequestInProgressException();
                case null, default -> throw e;
            }
        }
        return CustomerPersistenceAssembler.toDomainFromPersistence(entity);
    }
}
