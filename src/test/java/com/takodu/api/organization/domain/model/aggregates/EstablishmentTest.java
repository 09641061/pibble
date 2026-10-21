package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.model.valueobjects.IanaZone;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstablishmentTest {
    private final OrganizationId organization = new OrganizationId(UUID.randomUUID());

    @Test
    void blankAddressIsStoredAsNone() {
        var establishment = Establishment.open(organization, "Sede", "  ", IanaZone.LIMA, null);

        assertNull(establishment.getAddress());
    }

    @Test
    void blankNameIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Establishment.open(organization, " ", null, IanaZone.LIMA, null));
    }

    @Test
    void archivingTwiceKeepsTheFirstInstant() {
        var establishment = Establishment.open(organization, "Sede", null, IanaZone.LIMA, null);
        var first = Instant.parse("2026-10-20T10:00:00Z");

        establishment.archive(first);
        establishment.archive(first.plusSeconds(60));

        assertEquals(first, establishment.getArchivedAt());
        assertFalse(establishment.isActive());
    }
}
