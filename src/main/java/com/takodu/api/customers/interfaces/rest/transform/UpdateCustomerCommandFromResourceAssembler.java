package com.takodu.api.customers.interfaces.rest.transform;

import com.takodu.api.customers.domain.model.commands.UpdateCustomerCommand;
import com.takodu.api.customers.domain.model.valueobjects.IdentityDocument;
import com.takodu.api.customers.interfaces.rest.resources.CustomerDetailsResource;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.UUID;

public final class UpdateCustomerCommandFromResourceAssembler {
    private UpdateCustomerCommandFromResourceAssembler() { }

    public static UpdateCustomerCommand toCommandFromResource(OrganizationId organizationId, UUID customerId,
                                                              CustomerDetailsResource resource) {
        return new UpdateCustomerCommand(organizationId, new CustomerId(customerId), resource.name(),
                IdentityDocument.optional(resource.documentType(), resource.documentNumber()), resource.notes());
    }
}
