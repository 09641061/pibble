package com.takodu.api.booking.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.Money;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;

import java.time.Duration;
import java.util.List;

/**
 * A service as booking sees it (B1, B2, B8). Built by {@code ExternalOrganizationService} from organization's facade.
 *
 * @param performers the members who can perform it now (bookable, active, working at the establishment), in the
 *        team's order
 */
public record ServiceOffer(ServiceId id, EstablishmentId establishmentId, Duration duration, Money price,
                           boolean active, List<MemberId> performers) {
    public ServiceOffer {
        if (id == null || establishmentId == null || duration == null || price == null)
            throw new IllegalArgumentException("id, establishmentId, duration and price are required");
        performers = List.copyOf(performers);
    }

    /** B2 */
    public boolean isPerformedBy(MemberId member) {
        return performers.contains(member);
    }
}
