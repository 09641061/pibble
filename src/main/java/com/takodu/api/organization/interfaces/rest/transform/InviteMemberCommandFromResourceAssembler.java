package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.commands.InviteMemberCommand;
import com.takodu.api.organization.interfaces.rest.resources.InviteMemberResource;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.Role;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class InviteMemberCommandFromResourceAssembler {
    private InviteMemberCommandFromResourceAssembler() { }

    public static InviteMemberCommand toCommandFromResource(OrganizationId organizationId, MemberId inviterId,
                                                            UUID idempotencyKey, InviteMemberResource resource) {
        return new InviteMemberCommand(organizationId, inviterId, new Email(resource.email()), resource.displayName(),
                role(resource.role()), establishmentIds(resource.establishmentIds()),
                resource.bookable() == null || resource.bookable(), new IdempotencyKey(idempotencyKey));
    }

    static Role role(String role) {
        try {
            return Role.valueOf(role.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role must be owner, manager or staff");
        }
    }

    static Set<EstablishmentId> establishmentIds(Set<UUID> ids) {
        return ids == null ? Set.of() : ids.stream().map(EstablishmentId::new).collect(Collectors.toSet());
    }
}
