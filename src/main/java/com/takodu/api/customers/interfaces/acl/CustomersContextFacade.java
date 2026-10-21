package com.takodu.api.customers.interfaces.acl;

import java.util.Collection;
import java.util.List;
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

    /**
     * @param customerIds at most 200, such as the senders of one inbox page
     * @return the customers found, deleted or not, in no particular order; ids of other organizations are skipped
     */
    List<CustomerView> fetchCustomers(UUID organizationId, Collection<UUID> customerIds);
}
