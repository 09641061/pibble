package com.takodu.api.booking.domain.model.aggregates;

import com.takodu.api.booking.domain.model.valueobjects.TimeBlockId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import lombok.Getter;

import java.time.Duration;
import java.util.Objects;

/**
 * B4, B15: a range in which one member, or the whole establishment, cannot be booked. Creating it cancels nothing;
 * the calendar shows the appointments it overlaps.
 */
@Getter
public class TimeBlock {
    public static final Duration MAX_LENGTH = Duration.ofDays(366);
    public static final int MAX_REASON = 500;

    private final TimeBlockId id;
    private final OrganizationId organizationId;
    private final EstablishmentId establishmentId;
    private final MemberId staffMemberId;
    private final TimeRange range;
    private final String reason;
    private final IdempotencyKey idempotencyKey;

    /**
     * @param staffMemberId the blocked member; null blocks the whole establishment
     * @param reason optional, such as "vacaciones"
     */
    public static TimeBlock create(OrganizationId organizationId, EstablishmentId establishmentId, MemberId staffMemberId,
                                   TimeRange range, String reason, IdempotencyKey idempotencyKey) {
        if (range == null) throw new IllegalArgumentException("start and end are required");
        if (range.length().compareTo(MAX_LENGTH) > 0)
            throw new IllegalArgumentException("A time block lasts at most 366 days");
        return new TimeBlock(null, organizationId, establishmentId, staffMemberId, range, reason, idempotencyKey);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public TimeBlock(TimeBlockId id, OrganizationId organizationId, EstablishmentId establishmentId,
                     MemberId staffMemberId, TimeRange range, String reason, IdempotencyKey idempotencyKey) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.establishmentId = Objects.requireNonNull(establishmentId, "establishmentId is required");
        this.staffMemberId = staffMemberId;
        this.range = Objects.requireNonNull(range, "range is required");
        this.reason = requireReason(reason);
        this.idempotencyKey = idempotencyKey;
    }

    /** @return whether the block applies to every member of the establishment */
    public boolean coversWholeEstablishment() {
        return staffMemberId == null;
    }

    /** B4: whether the block keeps the member from being booked in the candidate range. */
    public boolean blocks(MemberId member, TimeRange candidate) {
        return (coversWholeEstablishment() || staffMemberId.equals(member)) && range.overlaps(candidate);
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        if (reason.length() > MAX_REASON) throw new IllegalArgumentException("Reason must have at most 500 characters");
        return reason.strip();
    }
}
