package com.takodu.api.conversations.application.internal.outboundservices.acl;

import com.takodu.api.conversations.domain.model.valueobjects.CustomerContact;
import com.takodu.api.customers.interfaces.acl.CustomerView;
import com.takodu.api.customers.interfaces.acl.CustomersContextFacade;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

/** The only class in conversations that talks to customers. */
@ApplicationScoped
public class ExternalCustomersService {
    private final CustomersContextFacade customersContextFacade;

    public ExternalCustomersService(CustomersContextFacade customersContextFacade) {
        this.customersContextFacade = customersContextFacade;
    }

    /**
     * C2: the customer behind a WhatsApp number, created on their first message.
     *
     * @param waId the number without {@code +}, as Meta sends it
     */
    public CustomerId findOrCreateByWhatsapp(OrganizationId organizationId, String waId, String profileName) {
        return new CustomerId(customersContextFacade.findOrCreateByWhatsapp(organizationId.value(), waId, profileName).id());
    }

    /** @return the customers found, deleted or not, by id; ids of other organizations are missing */
    public Map<CustomerId, CustomerContact> fetchContacts(OrganizationId organizationId, Collection<CustomerId> customerIds) {
        if (customerIds.isEmpty()) return Map.of();
        return customersContextFacade.fetchCustomers(organizationId.value(),
                        customerIds.stream().map(CustomerId::value).distinct().toList()).stream()
                .map(ExternalCustomersService::contact)
                .collect(toMap(CustomerContact::customerId, contact -> contact));
    }

    /** @return the customer, deleted or not; empty when not in the organization */
    public Optional<CustomerContact> fetchContact(OrganizationId organizationId, CustomerId customerId) {
        return customersContextFacade.fetchCustomer(organizationId.value(), customerId.value())
                .map(ExternalCustomersService::contact);
    }

    private static CustomerContact contact(CustomerView view) {
        return new CustomerContact(new CustomerId(view.id()), view.phone(), view.displayName(), view.deleted());
    }
}
