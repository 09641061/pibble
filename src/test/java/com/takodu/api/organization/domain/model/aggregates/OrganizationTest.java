package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.exceptions.PlanLimitReachedException;
import com.takodu.api.organization.domain.model.valueobjects.Plan;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationTest {
    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

    @Test
    void newOrganizationStartsOnStandardWithItsTrial() {
        var organization = Organization.create("  Barbería Ñandú 2 ", NOW.plusSeconds(60), new IdempotencyKey(UUID.randomUUID()));

        assertEquals("Barbería Ñandú 2", organization.getName());
        assertEquals(Plan.STANDARD, organization.getPlan());
        assertTrue(organization.isPlanActive(NOW));
    }

    @Test
    void blankNameIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> Organization.create(" ", NOW, new IdempotencyKey(UUID.randomUUID())));
    }

    @Test
    void planIsInactiveFromItsEnd() {
        var organization = organization(Plan.STANDARD, NOW);

        assertFalse(organization.isPlanActive(NOW));
        assertThrows(PlanInactiveException.class, () -> organization.requireActivePlan(NOW));
    }

    @Test
    void standardAllowsOneActiveEstablishment() {
        var organization = organization(Plan.STANDARD, NOW.plusSeconds(60));

        assertDoesNotThrow(() -> organization.ensureCanAddEstablishment(0));
        assertThrows(PlanLimitReachedException.class, () -> organization.ensureCanAddEstablishment(1));
    }

    @Test
    void downgradedOrganizationKeepsItsBranchesButOpensNoMore() {
        var organization = organization(Plan.STANDARD, NOW.plusSeconds(60));

        assertThrows(PlanLimitReachedException.class, () -> organization.ensureCanAddEstablishment(3));
    }

    @Test
    void maxAllowsManyEstablishments() {
        assertDoesNotThrow(() -> organization(Plan.MAX, NOW.plusSeconds(60)).ensureCanAddEstablishment(40));
    }

    @Test
    void changingThePlanRecordsWhereItCameFrom() {
        var organization = organization(Plan.MAX, NOW);
        var operator = new UserId(UUID.randomUUID());
        var until = NOW.plus(Duration.ofDays(30));

        var change = organization.changePlan(Plan.STANDARD, until, operator, " Yape 2026-10-23 ",
                new IdempotencyKey(UUID.randomUUID()), NOW);

        assertEquals(Plan.STANDARD, organization.getPlan());
        assertEquals(until, organization.getPlanActiveUntil());
        assertEquals(Plan.MAX, change.getFromPlan());
        assertEquals(NOW, change.getFromActiveUntil());
        assertEquals(operator, change.getChangedBy());
        assertEquals("Yape 2026-10-23", change.getNote());
    }

    @Test
    void aPlanChangeNeedsANote() {
        var organization = organization(Plan.STANDARD, NOW);

        assertThrows(IllegalArgumentException.class, () -> organization.changePlan(Plan.MAX, NOW,
                new UserId(UUID.randomUUID()), " ", new IdempotencyKey(UUID.randomUUID()), NOW));
        assertEquals(Plan.STANDARD, organization.getPlan());
    }

    @Test
    void extendingCountsFromTheEndWhileActiveAndFromNowAfter() {
        var active = organization(Plan.STANDARD, NOW.plus(Duration.ofDays(5)));
        var ended = organization(Plan.STANDARD, NOW.minus(Duration.ofDays(5)));

        assertEquals(NOW.plus(Duration.ofDays(35)), active.planEndExtendedBy(30, NOW));
        assertEquals(NOW.plus(Duration.ofDays(30)), ended.planEndExtendedBy(30, NOW));
        assertThrows(IllegalArgumentException.class, () -> active.planEndExtendedBy(0, NOW));
    }

    private static Organization organization(Plan plan, Instant activeUntil) {
        return new Organization(new OrganizationId(UUID.randomUUID()), "Spa", plan, activeUntil, null, null);
    }
}
