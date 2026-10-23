package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.model.valueobjects.IanaZone;
import com.takodu.api.organization.domain.model.valueobjects.WeeklySchedule;
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
    private WeeklySchedule schedule;
    private Instant archivedAt;
    private final IdempotencyKey idempotencyKey;

    /**
     * Opens a branch with no opening hours, so it is closed until hours are set (O8).
     *
     * @param address optional street address
     */
    public static Establishment open(OrganizationId organizationId, String name, String address, IanaZone timeZone,
                                     IdempotencyKey idempotencyKey) {
        return new Establishment(null, organizationId, name, address, timeZone, WeeklySchedule.CLOSED, null,
                idempotencyKey);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Establishment(EstablishmentId id, OrganizationId organizationId, String name, String address,
                         IanaZone timeZone, WeeklySchedule schedule, Instant archivedAt, IdempotencyKey idempotencyKey) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.name = requireName(name);
        this.address = blankToNull(address);
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone is required");
        this.schedule = Objects.requireNonNull(schedule, "schedule is required");
        this.archivedAt = archivedAt;
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * Changes the name, address and time zone. Existing appointments keep their instants; opening hours are read in
     * the new zone (O7).
     */
    public void describe(String name, String address, IanaZone timeZone) {
        this.name = requireName(name);
        this.address = blankToNull(address);
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone is required");
    }

    /**
     * O8: replaces the whole week. Appointments already booked outside the new hours stay; the calendar shows them.
     */
    public void replaceSchedule(WeeklySchedule schedule) {
        this.schedule = Objects.requireNonNull(schedule, "schedule is required");
    }

    /** O4: archiving frees the establishment's place in the plan. Archiving twice keeps the first instant. */
    public void archive(Instant now) {
        if (archivedAt == null) archivedAt = now;
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
