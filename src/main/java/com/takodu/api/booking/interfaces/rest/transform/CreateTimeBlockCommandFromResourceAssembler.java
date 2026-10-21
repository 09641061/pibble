package com.takodu.api.booking.interfaces.rest.transform;

import com.takodu.api.booking.domain.model.commands.CreateTimeBlockCommand;
import com.takodu.api.booking.interfaces.rest.resources.CreateTimeBlockResource;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;

import java.util.UUID;

public final class CreateTimeBlockCommandFromResourceAssembler {
    private CreateTimeBlockCommandFromResourceAssembler() { }

    public static CreateTimeBlockCommand toCommandFromResource(OrganizationId organizationId, MemberId requestedBy,
                                                               UUID establishmentId, UUID idempotencyKey,
                                                               CreateTimeBlockResource resource) {
        return new CreateTimeBlockCommand(organizationId, new EstablishmentId(establishmentId),
                resource.staffMemberId() == null ? null : new MemberId(resource.staffMemberId()),
                new TimeRange(resource.start(), resource.end()), resource.reason(), requestedBy,
                new IdempotencyKey(idempotencyKey));
    }
}
