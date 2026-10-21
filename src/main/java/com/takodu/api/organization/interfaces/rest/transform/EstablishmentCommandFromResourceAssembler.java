package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.commands.DescribeEstablishmentCommand;
import com.takodu.api.organization.domain.model.commands.OpenEstablishmentCommand;
import com.takodu.api.organization.domain.model.valueobjects.IanaZone;
import com.takodu.api.organization.interfaces.rest.resources.EstablishmentDetailsResource;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.UUID;

public final class EstablishmentCommandFromResourceAssembler {
    private EstablishmentCommandFromResourceAssembler() { }

    public static OpenEstablishmentCommand toOpenCommandFromResource(OrganizationId organizationId, UUID idempotencyKey,
                                                                     EstablishmentDetailsResource resource) {
        return new OpenEstablishmentCommand(organizationId, resource.name(), resource.address(),
                IanaZone.of(resource.timeZone()), new IdempotencyKey(idempotencyKey));
    }

    public static DescribeEstablishmentCommand toDescribeCommandFromResource(OrganizationId organizationId,
                                                                             UUID establishmentId,
                                                                             EstablishmentDetailsResource resource) {
        return new DescribeEstablishmentCommand(organizationId, new EstablishmentId(establishmentId), resource.name(),
                resource.address(), IanaZone.of(resource.timeZone()));
    }
}
