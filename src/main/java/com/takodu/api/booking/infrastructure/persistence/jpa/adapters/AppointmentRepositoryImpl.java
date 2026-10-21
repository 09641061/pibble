package com.takodu.api.booking.infrastructure.persistence.jpa.adapters;

import com.takodu.api.booking.domain.exceptions.AppointmentChangedException;
import com.takodu.api.booking.domain.exceptions.SlotUnavailableException;
import com.takodu.api.booking.domain.model.aggregates.Appointment;
import com.takodu.api.booking.domain.model.valueobjects.AppointmentId;
import com.takodu.api.booking.domain.repositories.AppointmentRepository;
import com.takodu.api.booking.infrastructure.persistence.jpa.assemblers.AppointmentPersistenceAssembler;
import com.takodu.api.booking.infrastructure.persistence.jpa.repositories.AppointmentPersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AppointmentRepositoryImpl implements AppointmentRepository {
    private static final String STAFF_NO_OVERLAP = "appointments_staff_no_overlap";
    private static final String CUSTOMER_NO_OVERLAP = "appointments_customer_no_overlap";
    private static final String IDEMPOTENCY_UNIQUE = "appointments_idempotency_uk";

    private final AppointmentPersistenceRepository appointmentPersistenceRepository;

    public AppointmentRepositoryImpl(AppointmentPersistenceRepository appointmentPersistenceRepository) {
        this.appointmentPersistenceRepository = appointmentPersistenceRepository;
    }

    @Override
    public Optional<Appointment> findById(OrganizationId organizationId, AppointmentId appointmentId) {
        return appointmentPersistenceRepository.findById(organizationId.value(), appointmentId.value())
                .map(AppointmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Appointment> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey) {
        return appointmentPersistenceRepository.findByIdempotencyKey(organizationId.value(), idempotencyKey.value())
                .map(AppointmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Appointment> findOccupying(OrganizationId organizationId, Collection<MemberId> memberIds,
                                           CustomerId customerId, TimeRange range) {
        var members = memberIds.stream().map(MemberId::value).toList();
        return appointmentPersistenceRepository.findOccupying(organizationId.value(), members,
                        customerId == null ? null : customerId.value(), range.start(), range.end()).stream()
                .map(AppointmentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<Appointment> findByEstablishment(OrganizationId organizationId, EstablishmentId establishmentId,
                                                 TimeRange window) {
        return appointmentPersistenceRepository.findByEstablishment(organizationId.value(), establishmentId.value(),
                        window.start(), window.end()).stream()
                .map(AppointmentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Appointment save(Appointment appointment) {
        var entity = AppointmentPersistenceAssembler.toPersistenceFromDomain(appointment);
        try {
            if (entity.getId() == null) {
                appointmentPersistenceRepository.persist(entity);
            } else {
                entity = appointmentPersistenceRepository.getEntityManager().merge(entity);
            }
            appointmentPersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (constraintOf(e)) {
                case STAFF_NO_OVERLAP -> throw new SlotUnavailableException("The member is busy then");
                case CUSTOMER_NO_OVERLAP -> throw new SlotUnavailableException("The customer has another appointment then");
                case IDEMPOTENCY_UNIQUE -> throw new RequestInProgressException();
                case null, default -> throw e;
            }
        } catch (OptimisticLockException e) {
            throw new AppointmentChangedException(appointment.getId());
        }
        return AppointmentPersistenceAssembler.toDomainFromPersistence(entity);
    }

    /**
     * Hibernate names unique and check constraints but not exclusion constraints (SQL state {@code 23P01}), so their
     * name is read from PostgreSQL's error.
     */
    private static String constraintOf(ConstraintViolationException e) {
        if (e.getConstraintName() != null) return e.getConstraintName();
        if (e.getSQLException() instanceof PSQLException postgres && postgres.getServerErrorMessage() != null)
            return postgres.getServerErrorMessage().getConstraint();
        return null;
    }
}
