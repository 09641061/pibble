package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.commands.CreateServiceCommand;
import com.takodu.api.organization.domain.model.commands.UpdateServiceCommand;
import com.takodu.api.organization.interfaces.rest.resources.ServiceDetailsResource;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ServiceCommandFromResourceAssembler {
    private ServiceCommandFromResourceAssembler() { }

    public static CreateServiceCommand toCreateCommandFromResource(OrganizationId organizationId, UUID establishmentId,
                                                                   UUID idempotencyKey, ServiceDetailsResource resource) {
        return new CreateServiceCommand(organizationId, new EstablishmentId(establishmentId), resource.name(),
                Duration.ofMinutes(resource.durationMinutes()), Money.pen(resource.priceCents()), resource.instructions(),
                performerIds(resource.performerIds()), new IdempotencyKey(idempotencyKey));
    }

    public static UpdateServiceCommand toUpdateCommandFromResource(OrganizationId organizationId, UUID serviceId,
                                                                   ServiceDetailsResource resource) {
        return new UpdateServiceCommand(organizationId, new ServiceId(serviceId), resource.name(),
                Duration.ofMinutes(resource.durationMinutes()), Money.pen(resource.priceCents()), resource.instructions(),
                performerIds(resource.performerIds()));
    }

    private static Set<MemberId> performerIds(Set<UUID> ids) {
        return ids == null ? Set.of() : ids.stream().map(MemberId::new).collect(Collectors.toSet());
    }
}
