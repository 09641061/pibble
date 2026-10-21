package com.takodu.api.booking.interfaces.rest.transform;

import com.takodu.api.booking.domain.model.aggregates.TimeBlock;
import com.takodu.api.booking.interfaces.rest.resources.TimeBlockResource;

public final class TimeBlockResourceFromEntityAssembler {
    private TimeBlockResourceFromEntityAssembler() { }

    public static TimeBlockResource toResourceFromEntity(TimeBlock timeBlock) {
        return new TimeBlockResource(timeBlock.getId().value(), timeBlock.getEstablishmentId().value(),
                timeBlock.getStaffMemberId() == null ? null : timeBlock.getStaffMemberId().value(),
                timeBlock.getRange().start(), timeBlock.getRange().end(), timeBlock.getReason());
    }
}
