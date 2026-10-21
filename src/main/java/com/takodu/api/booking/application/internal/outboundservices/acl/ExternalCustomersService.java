package com.takodu.api.booking.application.internal.outboundservices.acl;

import com.takodu.api.booking.domain.exceptions.CustomerNotFoundException;
import com.takodu.api.customers.interfaces.acl.CustomersContextFacade;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

/** The only class in booking that talks to customers. */
@ApplicationScoped
public class ExternalCustomersService {
    private final CustomersContextFacade customersContextFacade;

    public ExternalCustomersService(CustomersContextFacade customersContextFacade) {
        this.customersContextFacade = customersContextFacade;
    }

    /** @throws CustomerNotFoundException when the customer is deleted or not in the organization (C5) */
    public void requireActiveCustomer(OrganizationId organizationId, CustomerId customerId) {
        customersContextFacade.fetchCustomer(organizationId.value(), customerId.value())
                .filter(customer -> !customer.deleted())
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
