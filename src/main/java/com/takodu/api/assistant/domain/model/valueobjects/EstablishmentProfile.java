package com.takodu.api.assistant.domain.model.valueobjects;

import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * What the assistant knows about the establishment it answers for.
 *
 * @param address null when the owner did not set one
 * @param zone where the customer reads every date and time (O7)
 * @param openingHours every range of the week; a day without ranges is closed (O8)
 * @param archived whether it is archived (O4)
 * @param planActive whether the organization's plan is active now (O6)
 */
public record EstablishmentProfile(EstablishmentId establishmentId, String name, String address, ZoneId zone,
                                   List<OpeningRange> openingHours, boolean archived, boolean planActive) {
    public EstablishmentProfile {
        Objects.requireNonNull(establishmentId, "establishmentId is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(zone, "zone is required");
        openingHours = List.copyOf(openingHours);
    }
}
