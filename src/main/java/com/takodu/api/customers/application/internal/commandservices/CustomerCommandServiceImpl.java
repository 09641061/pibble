package com.takodu.api.customers.application.internal.commandservices;

import com.takodu.api.customers.application.commandservices.CustomerCommandService;
import com.takodu.api.customers.domain.exceptions.CustomerAlreadyRegisteredException;
import com.takodu.api.customers.domain.exceptions.CustomerNotFoundException;
import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.commands.DeleteCustomerCommand;
import com.takodu.api.customers.domain.model.commands.RecordWhatsappContactCommand;
import com.takodu.api.customers.domain.model.commands.RegisterCustomerCommand;
import com.takodu.api.customers.domain.model.commands.UpdateCustomerCommand;
import com.takodu.api.customers.domain.repositories.CustomerRepository;
import com.takodu.api.customers.interfaces.events.CustomerDeletedIntegrationEvent;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import java.time.Clock;

@ApplicationScoped
public class CustomerCommandServiceImpl implements CustomerCommandService {
    private final CustomerRepository customerRepository;
    private final AccessPolicy accessPolicy;
    private final Clock clock;
    private final Event<CustomerDeletedIntegrationEvent> customerDeleted;

    public CustomerCommandServiceImpl(CustomerRepository customerRepository, AccessPolicy accessPolicy, Clock clock,
                                      Event<CustomerDeletedIntegrationEvent> customerDeleted) {
        this.customerRepository = customerRepository;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
        this.customerDeleted = customerDeleted;
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

    @Override
    @Transactional
    public Customer handle(UpdateCustomerCommand command) {
        accessPolicy.require(Action.EDIT_CUSTOMERS);
        var customer = load(command.organizationId(), command.customerId());
        customer.updateDetails(command.name(), command.document(), command.notes());
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void handle(DeleteCustomerCommand command) {
        accessPolicy.require(Action.DELETE_CUSTOMERS);
        var customer = load(command.organizationId(), command.customerId());
        customer.delete(clock.instant());
        customerRepository.save(customer);
        customerDeleted.fire(new CustomerDeletedIntegrationEvent(command.organizationId().value(),
                command.customerId().value()));
    }

    @Override
    @Transactional
    public Customer handle(RecordWhatsappContactCommand command) {
        var existing = customerRepository.findActiveByPhone(command.organizationId(), command.phone());
        if (existing.isEmpty())
            return customerRepository.addIfPhoneFree(Customer.fromWhatsapp(command.organizationId(), command.phone(),
                    command.profileName()));
        var customer = existing.get();
        return customer.refreshProfileName(command.profileName()) ? customerRepository.save(customer) : customer;
    }

    private Customer load(OrganizationId organizationId, CustomerId customerId) {
        return customerRepository.findActiveById(organizationId, customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
