package com.takodu.api.customers.interfaces.acl;

import java.util.Optional;
import java.util.UUID;

/** What other contexts may ask customers. */
public interface CustomersContextFacade {

    /**
     * C2: the customer behind an inbound WhatsApp message, created on the first one. Safe to call twice and
     * concurrently for one number. Refreshes the WhatsApp profile name, never the name staff typed.
     *
     * @param organizationId the organization of the WhatsApp line that received the message
     * @param waId Meta's sender id, the number without {@code +}, such as {@code 51987654321}
     * @param profileName the sender's WhatsApp profile name, optional
     */
    CustomerView findOrCreateByWhatsapp(UUID organizationId, String waId, String profileName);

    /** @return the customer even when deleted, empty when not in the organization */
    Optional<CustomerView> fetchCustomer(UUID organizationId, UUID customerId);
}
