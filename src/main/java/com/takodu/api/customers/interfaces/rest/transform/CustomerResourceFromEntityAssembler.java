package com.takodu.api.customers.interfaces.rest.transform;

import com.takodu.api.customers.application.queryservices.CustomerPage;
import com.takodu.api.customers.domain.model.aggregates.Customer;
import com.takodu.api.customers.interfaces.rest.resources.CustomerPageResource;
import com.takodu.api.customers.interfaces.rest.resources.CustomerResource;

import java.util.Locale;

public final class CustomerResourceFromEntityAssembler {
    private CustomerResourceFromEntityAssembler() { }

    public static CustomerResource toResourceFromEntity(Customer customer) {
        var document = customer.getDocument();
        return new CustomerResource(customer.getId().value(), customer.getPhone().e164(), customer.displayName(),
                customer.getName(), customer.getWhatsappProfileName(),
                document == null ? null : document.type().name().toLowerCase(Locale.ROOT),
                document == null ? null : document.number(), customer.getNotes());
    }

    public static CustomerPageResource toPageResourceFromEntity(CustomerPage customers, int page, int size) {
        return new CustomerPageResource(customers.items().stream()
                .map(CustomerResourceFromEntityAssembler::toResourceFromEntity)
                .toList(), page, size, customers.hasMore());
    }
}
