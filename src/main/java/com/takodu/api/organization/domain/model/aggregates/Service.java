package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.exceptions.PerformerNotEligibleException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import lombok.Getter;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * O14, O15: something an establishment sells, and the members who perform it. Appointments copy the price when
 * booked (B8), so changing or deactivating a service never changes them.
 */
@Getter
public class Service {
    public static final Duration MAX_DURATION = Duration.ofHours(12);
    public static final int MAX_INSTRUCTIONS = 1000;

    private final ServiceId id;
    private final OrganizationId organizationId;
    private final EstablishmentId establishmentId;
    private String name;
    private Duration duration;
    private Money price;
    private String instructions;
    private boolean active;
    private Set<MemberId> performerIds;
    private final IdempotencyKey idempotencyKey;

    /** @param instructions optional text sent to the customer, such as "come with clean hair" */
    public static Service create(OrganizationId organizationId, EstablishmentId establishmentId, String name,
                                 Duration duration, Money price, String instructions, IdempotencyKey idempotencyKey) {
        return new Service(null, organizationId, establishmentId, name, duration, price, instructions, true, Set.of(),
                idempotencyKey);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Service(ServiceId id, OrganizationId organizationId, EstablishmentId establishmentId, String name,
                   Duration duration, Money price, String instructions, boolean active, Set<MemberId> performerIds,
                   IdempotencyKey idempotencyKey) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.establishmentId = Objects.requireNonNull(establishmentId, "establishmentId is required");
        this.name = requireName(name);
        this.duration = requireDuration(duration);
        this.price = Objects.requireNonNull(price, "price is required");
        this.instructions = requireInstructions(instructions);
        this.active = active;
        this.performerIds = Set.copyOf(performerIds);
        this.idempotencyKey = idempotencyKey;
    }

    /** O14: name, duration in whole minutes (more than zero), price and instructions. */
    public void describe(String name, Duration duration, Money price, String instructions) {
        this.name = requireName(name);
        this.duration = requireDuration(duration);
        this.price = Objects.requireNonNull(price, "price is required");
        this.instructions = requireInstructions(instructions);
    }

    /**
     * O15: replaces who performs the service.
     *
     * @param performers the members, loaded from this organization
     * @throws PerformerNotEligibleException when one of them is not bookable, has not joined, or does not work at
     *         this establishment
     */
    public void assignPerformers(Collection<Member> performers) {
        for (var member : performers)
            if (!member.canPerformAt(establishmentId)) throw new PerformerNotEligibleException(member.getId());
        this.performerIds = performers.stream().map(Member::getId).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Drops the member when they can no longer perform here (removed, not bookable, moved to another establishment).
     *
     * @return whether the service changed
     */
    public boolean dropIfIneligible(Member member) {
        if (!performerIds.contains(member.getId()) || member.canPerformAt(establishmentId)) return false;
        this.performerIds = performerIds.stream().filter(id -> !id.equals(member.getId()))
                .collect(Collectors.toUnmodifiableSet());
        return true;
    }

    /** O14: an inactive service is not offered; existing appointments keep their snapshot. */
    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Service name must not be blank");
        return name.strip();
    }

    private static Duration requireDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative())
            throw new IllegalArgumentException("Duration must be positive");
        if (duration.toSecondsPart() != 0 || duration.compareTo(MAX_DURATION) > 0)
            throw new IllegalArgumentException("Duration must be whole minutes, at most 12 hours");
        return duration;
    }

    private static String requireInstructions(String instructions) {
        if (instructions == null || instructions.isBlank()) return null;
        if (instructions.length() > MAX_INSTRUCTIONS)
            throw new IllegalArgumentException("Instructions must have at most 1000 characters");
        return instructions.strip();
    }
}
