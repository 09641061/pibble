package com.takodu.api.conversations.application.internal.outboundservices.acl;

import com.takodu.api.customers.interfaces.acl.CustomersContextFacade;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;

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
}
