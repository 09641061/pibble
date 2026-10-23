package com.takodu.api.conversations.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** C5: the customer was deleted, so the media they sent is deleted too, by the retention job. */
public record RemoveCustomerMediaCommand(OrganizationId organizationId, CustomerId customerId) {
    public RemoveCustomerMediaCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (customerId == null) throw new IllegalArgumentException("customerId is required");
    }
}
