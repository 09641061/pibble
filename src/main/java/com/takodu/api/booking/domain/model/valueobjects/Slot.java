package com.takodu.api.booking.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;

import java.util.List;

/**
 * B16: a time at which a service can be booked.
 *
 * @param freeMembers who can take it, in the order B17 picks them: least busy that day, then the team's order
 */
public record Slot(TimeRange range, List<MemberId> freeMembers) {
    public Slot {
        if (range == null) throw new IllegalArgumentException("range is required");
        freeMembers = List.copyOf(freeMembers);
    }
}
