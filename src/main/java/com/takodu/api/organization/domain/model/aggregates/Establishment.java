package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.model.valueobjects.IanaZone;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * A physical branch with its own address, time zone, opening hours, services, calendar and WhatsApp line.
 */
@Getter
public class Establishment {
    private final EstablishmentId id;
    private final OrganizationId organizationId;
    private String name;
    private String address;
    private IanaZone timeZone;
    private Instant archivedAt;
    private final IdempotencyKey idempotencyKey;

    /**
     * Opens a branch with no opening hours, so it is closed until hours are set (O8).
     *
     * @param address optional street address
     */
    public static Establishment open(OrganizationId organizationId, String name, String address, IanaZone timeZone,
                                     IdempotencyKey idempotencyKey) {
        return new Establishment(null, organizationId, name, address, timeZone, null, idempotencyKey);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Establishment(EstablishmentId id, OrganizationId organizationId, String name, String address,
                         IanaZone timeZone, Instant archivedAt, IdempotencyKey idempotencyKey) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.name = requireName(name);
        this.address = blankToNull(address);
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone is required");
        this.archivedAt = archivedAt;
        this.idempotencyKey = idempotencyKey;
    }

    /** @return whether the establishment is open for business and counts toward the plan limit (O4) */
    public boolean isActive() {
        return archivedAt == null;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Establishment name must not be blank");
        return name.strip();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
