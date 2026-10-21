package com.takodu.api.customers.application.internal.commandservices;

import com.takodu.api.customers.application.commandservices.CustomerCommandService;
import com.takodu.api.customers.domain.exceptions.CustomerAlreadyRegisteredException;
import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.commands.RegisterCustomerCommand;
import com.takodu.api.customers.domain.repositories.CustomerRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CustomerCommandServiceImpl implements CustomerCommandService {
    private final CustomerRepository customerRepository;
    private final AccessPolicy accessPolicy;

    public CustomerCommandServiceImpl(CustomerRepository customerRepository, AccessPolicy accessPolicy) {
        this.customerRepository = customerRepository;
        this.accessPolicy = accessPolicy;
    }

    @Override
    @Transactional
    public Customer handle(RegisterCustomerCommand command) {
        accessPolicy.require(Action.EDIT_CUSTOMERS);
        var earlier = customerRepository.findByIdempotencyKey(command.organizationId(), command.idempotencyKey());
        if (earlier.isPresent()) return earlier.get();
        if (customerRepository.findActiveByPhone(command.organizationId(), command.phone()).isPresent())
            throw new CustomerAlreadyRegisteredException(command.phone());
        return customerRepository.save(Customer.registerByStaff(command.organizationId(), command.phone(),
                command.name(), command.document(), command.notes(), command.idempotencyKey()));
    }
}
