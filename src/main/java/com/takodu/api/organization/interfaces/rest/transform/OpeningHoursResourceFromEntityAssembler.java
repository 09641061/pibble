package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.interfaces.rest.resources.OpeningHoursResource;
import com.takodu.api.organization.interfaces.rest.resources.OpeningRangeResource;

public final class OpeningHoursResourceFromEntityAssembler {
    private OpeningHoursResourceFromEntityAssembler() { }

    public static OpeningHoursResource toResourceFromEntity(Establishment establishment) {
        return new OpeningHoursResource(establishment.getSchedule().ranges().stream()
                .map(range -> new OpeningRangeResource(range.day().getValue(), range.opensAt(), range.closesAt()))
                .toList());
    }
}
