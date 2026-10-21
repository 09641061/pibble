package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.Role;

import java.util.Set;

/**
 * Changes a member's name, bookability, role and, for staff, establishments.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 * @param role the owner's role can only stay {@code owner} (O12)
 */
public record UpdateMemberCommand(OrganizationId organizationId, MemberId memberId, String displayName,
                                  boolean bookable, Role role, Set<EstablishmentId> establishmentIds) {
    public UpdateMemberCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (memberId == null) throw new IllegalArgumentException("memberId is required");
        if (role == null) throw new IllegalArgumentException("role is required");
        establishmentIds = establishmentIds == null ? Set.of() : Set.copyOf(establishmentIds);
    }
}
