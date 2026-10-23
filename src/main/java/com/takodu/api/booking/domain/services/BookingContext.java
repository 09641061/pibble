package com.takodu.api.booking.domain.services;

import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.aggregates.TimeBlock;
import com.takodu.api.booking.domain.model.valueobjects.EstablishmentCalendar;
import com.takodu.api.booking.domain.model.valueobjects.ServiceOffer;

import java.time.Instant;
import java.util.List;

/**
 * Everything {@link AvailabilityPolicy} needs, loaded by the application service in one transaction.
 *
 * @param blocks the establishment's time blocks overlapping the ranges being checked
 * @param occupying appointments of the candidate members and of the customer around the ranges being checked; for
 *        choosing a member, every appointment of the candidates on that local day
 * @param now the current instant
 */
public record BookingContext(EstablishmentCalendar calendar, ServiceOffer service, List<TimeBlock> blocks,
                             List<Appointment> occupying, Instant now) {
    public BookingContext {
        blocks = List.copyOf(blocks);
        occupying = List.copyOf(occupying);
    }
}
