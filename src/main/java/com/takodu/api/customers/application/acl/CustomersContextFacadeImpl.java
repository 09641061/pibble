package com.takodu.api.customers.application.acl;

import com.takodu.api.customers.application.commandservices.CustomerCommandService;
import com.takodu.api.customers.application.queryservices.CustomerQueryService;
import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.domain.model.commands.RecordWhatsappContactCommand;
import com.takodu.api.customers.domain.model.queries.GetCustomerIncludingDeletedQuery;
import com.takodu.api.customers.domain.model.queries.GetCustomersIncludingDeletedQuery;
import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.customers.interfaces.acl.CustomerView;
import com.takodu.api.customers.interfaces.acl.CustomersContextFacade;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

@ApplicationScoped
public class CustomersContextFacadeImpl implements CustomersContextFacade {
    private final CustomerCommandService customerCommandService;
    private final CustomerQueryService customerQueryService;

    public CustomersContextFacadeImpl(CustomerCommandService customerCommandService,
                                      CustomerQueryService customerQueryService) {
        this.customerCommandService = customerCommandService;
        this.customerQueryService = customerQueryService;
    }

    @Override
    public CustomerView findOrCreateByWhatsapp(UUID organizationId, String waId, String profileName) {
        return view(customerCommandService.handle(new RecordWhatsappContactCommand(new OrganizationId(organizationId),
                PhoneNumber.fromWhatsappId(waId), profileName)));
    }

    @Override
    public Optional<CustomerView> fetchCustomer(UUID organizationId, UUID customerId) {
        return customerQueryService.handle(new GetCustomerIncludingDeletedQuery(new OrganizationId(organizationId),
                new CustomerId(customerId))).map(CustomersContextFacadeImpl::view);
    }

    @Override
    public List<CustomerView> fetchCustomers(UUID organizationId, Collection<UUID> customerIds) {
        return customerQueryService.handle(new GetCustomersIncludingDeletedQuery(new OrganizationId(organizationId),
                        customerIds.stream().map(CustomerId::new).collect(toSet()))).stream()
                .map(CustomersContextFacadeImpl::view)
                .toList();
    }

    private static CustomerView view(Customer customer) {
        return new CustomerView(customer.getId().value(), customer.getPhone().e164(), customer.displayName(),
                customer.isDeleted());
    }
}
