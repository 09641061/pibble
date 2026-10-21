package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.exceptions.PerformerNotEligibleException;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.Role;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

    private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
    private final EstablishmentId branch = new EstablishmentId(UUID.randomUUID());
    private final EstablishmentId otherBranch = new EstablishmentId(UUID.randomUUID());

    @Test
    void staffOfTheEstablishmentAndManagersPerform() {
        var service = haircut();
        var staff = member(Role.STAFF, Set.of(branch), true, true);
        var manager = member(Role.MANAGER, Set.of(), true, true);

        service.assignPerformers(List.of(staff, manager));

        assertEquals(Set.of(staff.getId(), manager.getId()), service.getPerformerIds());
    }

    @Test
    void staffOfAnotherEstablishmentDoesNotPerform() {
        assertThrows(PerformerNotEligibleException.class,
                () -> haircut().assignPerformers(List.of(member(Role.STAFF, Set.of(otherBranch), true, true))));
    }

    @Test
    void memberWhoIsNotBookableDoesNotPerform() {
        assertThrows(PerformerNotEligibleException.class,
                () -> haircut().assignPerformers(List.of(member(Role.MANAGER, Set.of(), false, true))));
    }

    @Test
    void pendingInvitationDoesNotPerform() {
        assertThrows(PerformerNotEligibleException.class,
                () -> haircut().assignPerformers(List.of(member(Role.STAFF, Set.of(branch), true, false))));
    }

    @Test
    void movedStaffIsDroppedFromTheService() {
        var service = haircut();
        var staff = member(Role.STAFF, Set.of(branch), true, true);
        service.assignPerformers(List.of(staff));

        staff.assign(Role.STAFF, Set.of(otherBranch));

        assertTrue(service.dropIfIneligible(staff));
        assertTrue(service.getPerformerIds().isEmpty());
    }

    @Test
    void eligibleMemberStays() {
        var service = haircut();
        var staff = member(Role.STAFF, Set.of(branch), true, true);
        service.assignPerformers(List.of(staff));

        assertFalse(service.dropIfIneligible(staff));
    }

    @Test
    void durationIsWholePositiveMinutes() {
        var service = haircut();

        assertThrows(IllegalArgumentException.class, () -> service.describe("Corte", Duration.ZERO, Money.pen(0), null));
        assertThrows(IllegalArgumentException.class, () -> service.describe("Corte", Duration.ofSeconds(90), Money.pen(0), null));
        assertThrows(IllegalArgumentException.class, () -> service.describe("Corte", Duration.ofHours(13), Money.pen(0), null));
    }

    @Test
    void deactivatedServiceCanBeOfferedAgain() {
        var service = haircut();

        service.deactivate();
        assertFalse(service.isActive());
        service.activate();
        assertTrue(service.isActive());
    }

    private Service haircut() {
        return Service.create(organization, branch, "Corte", Duration.ofMinutes(45), Money.pen(3500), " ", null);
    }

    private Member member(Role role, Set<EstablishmentId> establishments, boolean bookable, boolean joined) {
        return new Member(new MemberId(UUID.randomUUID()), organization, joined ? new UserId(UUID.randomUUID()) : null,
                new Email("m" + UUID.randomUUID() + "@example.com"), "Ana", role, bookable,
                joined ? null : SecretHash.of("token"), joined ? null : NOW, joined ? NOW : null, null, establishments,
                null, null);
    }
}
