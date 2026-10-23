package com.takodu.api.organization.domain.model.commands;

import com.takodu.api.organization.domain.model.valueobjects.WeeklySchedule;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * O8: the whole week at once; a weekday left out is closed.
 *
 * @param organizationId the caller's organization, from {@code TenantContext}
 */
public record ReplaceOpeningHoursCommand(OrganizationId organizationId, EstablishmentId establishmentId,
                                         WeeklySchedule schedule) {
    public ReplaceOpeningHoursCommand {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (schedule == null) throw new IllegalArgumentException("schedule is required");
    }
}
