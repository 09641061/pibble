package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.commands.ChangePlanCommand;
import com.takodu.api.organization.domain.model.valueobjects.Plan;
import com.takodu.api.organization.interfaces.rest.resources.ChangePlanResource;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

import java.util.Locale;
import java.util.UUID;

public final class ChangePlanCommandFromResourceAssembler {
    private ChangePlanCommandFromResourceAssembler() { }

    public static ChangePlanCommand toCommandFromResource(OrganizationId organizationId, UserId operatorId,
                                                          UUID idempotencyKey, ChangePlanResource resource) {
        return new ChangePlanCommand(organizationId, operatorId,
                resource.plan() == null ? null : Plan.valueOf(resource.plan().toUpperCase(Locale.ROOT)),
                resource.activeUntil(), resource.extendDays(), resource.note(), new IdempotencyKey(idempotencyKey));
    }
}
