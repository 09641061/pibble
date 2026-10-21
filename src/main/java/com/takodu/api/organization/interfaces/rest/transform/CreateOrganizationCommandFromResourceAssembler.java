package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.commands.CreateOrganizationCommand;
import com.takodu.api.organization.domain.model.valueobjects.IanaZone;
import com.takodu.api.organization.interfaces.rest.resources.CreateOrganizationResource;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

import java.util.UUID;

public final class CreateOrganizationCommandFromResourceAssembler {
    private CreateOrganizationCommandFromResourceAssembler() { }

    public static CreateOrganizationCommand toCommandFromResource(UserId creatorId, UUID idempotencyKey,
                                                                  CreateOrganizationResource resource) {
        return new CreateOrganizationCommand(creatorId, resource.name(), resource.establishmentName(),
                resource.address(), IanaZone.of(resource.timeZone()), resource.ownerDisplayName(),
                new IdempotencyKey(idempotencyKey));
    }
}
