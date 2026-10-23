package com.takodu.api.customers.interfaces.rest.transform;

import com.takodu.api.customers.domain.model.commands.RegisterCustomerCommand;
import com.takodu.api.customers.domain.model.valueobjects.IdentityDocument;
import com.takodu.api.customers.domain.model.valueobjects.PhoneNumber;
import com.takodu.api.customers.interfaces.rest.resources.RegisterCustomerResource;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.UUID;

public final class RegisterCustomerCommandFromResourceAssembler {
    private RegisterCustomerCommandFromResourceAssembler() { }

    public static RegisterCustomerCommand toCommandFromResource(OrganizationId organizationId, UUID idempotencyKey,
                                                                RegisterCustomerResource resource) {
        return new RegisterCustomerCommand(organizationId, PhoneNumber.typed(resource.phone()), resource.name(),
                IdentityDocument.optional(resource.documentType(), resource.documentNumber()), resource.notes(),
                new IdempotencyKey(idempotencyKey));
    }
}
