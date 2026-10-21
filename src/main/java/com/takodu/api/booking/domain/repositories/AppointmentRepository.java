package com.takodu.api.booking.domain.repositories;

import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {
    Optional<Appointment> findById(OrganizationId organizationId, AppointmentId appointmentId);

    Optional<Appointment> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey);

    /**
     * B5, B6: the confirmed and in-progress appointments of these members, or of this customer, that overlap the range,
     * at any establishment.
     *
     * @param customerId null to look at the members only
     */
    List<Appointment> findOccupying(OrganizationId organizationId, Collection<MemberId> memberIds, CustomerId customerId,
                                    TimeRange range);

    /** @return the establishment's appointments in any status that overlap the window, earliest first */
    List<Appointment> findByEstablishment(OrganizationId organizationId, EstablishmentId establishmentId,
                                          TimeRange window);

    /**
     * @throws com.takodu.api.booking.domain.exceptions.SlotUnavailableException when an exclusion constraint rejects
     *         an overlap (B5, B6)
     * @throws com.takodu.api.booking.domain.exceptions.AppointmentChangedException when another transaction changed
     *         the appointment first
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    Appointment save(Appointment appointment);
}
