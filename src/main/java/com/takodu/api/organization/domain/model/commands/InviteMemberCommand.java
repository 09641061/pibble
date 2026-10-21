package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.Role;

import java.util.Set;

/**
 * O10: invite a person by email with a role.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 * @param inviterId the caller's membership, from {@code TenantContext}; named in the email
 * @param establishmentIds where a {@code staff} member works; ignored for managers
 */
public record InviteMemberCommand(OrganizationId organizationId, MemberId inviterId, Email email, String displayName,
                                  Role role, Set<EstablishmentId> establishmentIds, boolean bookable,
                                  IdempotencyKey idempotencyKey) {
    public InviteMemberCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (inviterId == null) throw new IllegalArgumentException("inviterId is required");
        if (email == null) throw new IllegalArgumentException("email is required");
        if (role == null) throw new IllegalArgumentException("role is required");
        if (idempotencyKey == null) throw new IllegalArgumentException("Idempotency-Key header is required");
        establishmentIds = establishmentIds == null ? Set.of() : Set.copyOf(establishmentIds);
    }
}
