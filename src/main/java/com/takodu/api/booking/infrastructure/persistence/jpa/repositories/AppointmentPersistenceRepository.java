package com.takodu.api.booking.infrastructure.persistence.jpa.repositories;

import com.takodu.api.booking.domain.model.valueobjects.AppointmentStatus;
import com.takodu.api.booking.infrastructure.persistence.jpa.entities.AppointmentPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AppointmentPersistenceRepository implements PanacheRepositoryBase<AppointmentPersistenceEntity, UUID> {
    /** B5, B6: the statuses the exclusion constraints guard. */
    private static final List<AppointmentStatus> OCCUPYING = List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS);

    public Optional<AppointmentPersistenceEntity> findById(UUID organizationId, UUID id) {
        return find("organizationId = ?1 and id = ?2", organizationId, id).firstResultOptional();
    }

    public Optional<AppointmentPersistenceEntity> findByIdempotencyKey(UUID organizationId, UUID key) {
        return find("organizationId = ?1 and idempotencyKey = ?2", organizationId, key).firstResultOptional();
    }

    /** @param customerId null to match the members only */
    public List<AppointmentPersistenceEntity> findOccupying(UUID organizationId, Collection<UUID> memberIds,
                                                            UUID customerId, Instant start, Instant end) {
        if (memberIds.isEmpty() && customerId == null) return List.of();
        var who = memberIds.isEmpty() ? "customerId = :customer"
                : customerId == null ? "staffMemberId in :members" : "(staffMemberId in :members or customerId = :customer)";
        var parameters = Parameters.with("organization", organizationId).and("statuses", OCCUPYING)
                .and("start", start).and("end", end);
        if (!memberIds.isEmpty()) parameters = parameters.and("members", memberIds);
        if (customerId != null) parameters = parameters.and("customer", customerId);
        return list("organizationId = :organization and " + who
                + " and status in :statuses and startsAt < :end and endsAt > :start order by startsAt, id", parameters);
    }

    public List<AppointmentPersistenceEntity> findUpcomingByCustomer(UUID organizationId, UUID establishmentId,
                                                                     UUID customerId, Instant after) {
        return list("organizationId = ?1 and establishmentId = ?2 and customerId = ?3 and status = ?4 and startsAt > ?5 "
                + "order by startsAt, id", organizationId, establishmentId, customerId, AppointmentStatus.CONFIRMED, after);
    }

    public List<AppointmentPersistenceEntity> findUpcomingByMember(UUID organizationId, UUID memberId, Instant after) {
        return list("organizationId = ?1 and staffMemberId = ?2 and status = ?3 and startsAt > ?4 order by startsAt, id",
                organizationId, memberId, AppointmentStatus.CONFIRMED, after);
    }

    public List<AppointmentPersistenceEntity> findByEstablishment(UUID organizationId, UUID establishmentId,
                                                                  Instant start, Instant end) {
        return list("organizationId = ?1 and establishmentId = ?2 and startsAt < ?4 and endsAt > ?3 order by startsAt, id",
                organizationId, establishmentId, start, end);
    }
}
