package com.takodu.api.shared.application.tenancy;

import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.Role;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessPolicyTest {
    private final EstablishmentId assigned = new EstablishmentId(UUID.randomUUID());
    private final EstablishmentId other = new EstablishmentId(UUID.randomUUID());

    @Test
    void managerMayManageMembersButNotTheOrganization() {
        var policy = policyFor(Role.MANAGER, Set.of());

        assertDoesNotThrow(() -> policy.require(Action.MANAGE_MEMBERS));
        var denied = assertThrows(AccessDeniedException.class, () -> policy.require(Action.MANAGE_ORGANIZATION));
        assertEquals("Not allowed to manage organization", denied.getMessage());
    }

    @Test
    void staffMayBookButNotDeleteCustomers() {
        var policy = policyFor(Role.STAFF, Set.of(assigned));

        assertDoesNotThrow(() -> policy.require(Action.BOOK_APPOINTMENTS));
        assertThrows(AccessDeniedException.class, () -> policy.require(Action.DELETE_CUSTOMERS));
    }

    @Test
    void staffSeeOnlyTheirAssignedEstablishments() {
        var policy = policyFor(Role.STAFF, Set.of(assigned));

        assertDoesNotThrow(() -> policy.requireEstablishment(assigned));
        assertThrows(AccessDeniedException.class, () -> policy.requireEstablishment(other));
    }

    @Test
    void ownerSeesEveryEstablishment() {
        var context = contextFor(Role.OWNER, Set.of(assigned));

        assertTrue(context.canSee(other));
        assertTrue(context.establishmentIds().isEmpty());
    }

    @Test
    void readingAnUnboundContextFails() {
        assertThrows(IllegalStateException.class, () -> new TenantContext().organizationId());
    }

    private AccessPolicy policyFor(Role role, Set<EstablishmentId> establishments) {
        return new AccessPolicy(contextFor(role, establishments));
    }

    private static TenantContext contextFor(Role role, Set<EstablishmentId> establishments) {
        var context = new TenantContext();
        context.bind(new UserId(UUID.randomUUID()), new OrganizationId(UUID.randomUUID()),
                new MemberId(UUID.randomUUID()), role, establishments);
        return context;
    }
}
