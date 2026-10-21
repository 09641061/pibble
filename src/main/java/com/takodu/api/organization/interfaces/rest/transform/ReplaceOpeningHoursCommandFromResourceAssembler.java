package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.commands.ReplaceOpeningHoursCommand;
import com.takodu.api.organization.domain.model.valueobjects.OpeningRange;
import com.takodu.api.organization.domain.model.valueobjects.WeeklySchedule;
import com.takodu.api.organization.interfaces.rest.resources.OpeningHoursResource;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.time.DayOfWeek;
import java.util.UUID;

public final class ReplaceOpeningHoursCommandFromResourceAssembler {
    private ReplaceOpeningHoursCommandFromResourceAssembler() { }

    public static ReplaceOpeningHoursCommand toCommandFromResource(OrganizationId organizationId, UUID establishmentId,
                                                                   OpeningHoursResource resource) {
        var schedule = new WeeklySchedule(resource.ranges().stream()
                .map(range -> new OpeningRange(DayOfWeek.of(range.weekday()), range.opensAt(), range.closesAt()))
                .toList());
        return new ReplaceOpeningHoursCommand(organizationId, new EstablishmentId(establishmentId), schedule);
    }
}
