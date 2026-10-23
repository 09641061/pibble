package com.takodu.api.booking.domain.model.commands;

import com.takodu.api.booking.domain.model.valueobjects.TimeBlockId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** @param requestedBy the caller's membership, from {@code TenantContext} */
public record DeleteTimeBlockCommand(OrganizationId organizationId, TimeBlockId timeBlockId, MemberId requestedBy) {
    public DeleteTimeBlockCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (timeBlockId == null) throw new IllegalArgumentException("timeBlockId is required");
        if (requestedBy == null) throw new IllegalArgumentException("requestedBy is required");
    }
}
