package com.takodu.api.booking.infrastructure.persistence.jpa.adapters;

import com.takodu.api.booking.domain.model.aggregates.TimeBlock;
import com.takodu.api.booking.domain.model.valueobjects.TimeBlockId;
import com.takodu.api.booking.domain.repositories.TimeBlockRepository;
import com.takodu.api.booking.infrastructure.persistence.jpa.assemblers.TimeBlockPersistenceAssembler;
import com.takodu.api.booking.infrastructure.persistence.jpa.repositories.TimeBlockPersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TimeBlockRepositoryImpl implements TimeBlockRepository {
    private static final String IDEMPOTENCY_UNIQUE = "time_blocks_idempotency_uk";

    private final TimeBlockPersistenceRepository timeBlockPersistenceRepository;

    public TimeBlockRepositoryImpl(TimeBlockPersistenceRepository timeBlockPersistenceRepository) {
        this.timeBlockPersistenceRepository = timeBlockPersistenceRepository;
    }

    @Override
    public Optional<TimeBlock> findById(OrganizationId organizationId, TimeBlockId timeBlockId) {
        return timeBlockPersistenceRepository.findById(organizationId.value(), timeBlockId.value())
                .map(TimeBlockPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<TimeBlock> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey) {
        return timeBlockPersistenceRepository.findByIdempotencyKey(organizationId.value(), idempotencyKey.value())
                .map(TimeBlockPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<TimeBlock> findOverlapping(OrganizationId organizationId, EstablishmentId establishmentId,
                                           TimeRange range) {
        return timeBlockPersistenceRepository.findOverlapping(organizationId.value(), establishmentId.value(),
                        range.start(), range.end()).stream()
                .map(TimeBlockPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public TimeBlock save(TimeBlock timeBlock) {
        var entity = TimeBlockPersistenceAssembler.toPersistenceFromDomain(timeBlock);
        try {
            if (entity.getId() == null) {
                timeBlockPersistenceRepository.persist(entity);
            } else {
                entity = timeBlockPersistenceRepository.getEntityManager().merge(entity);
            }
            timeBlockPersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (e.getConstraintName()) {
                case IDEMPOTENCY_UNIQUE -> throw new RequestInProgressException();
                case null, default -> throw e;
            }
        }
        return TimeBlockPersistenceAssembler.toDomainFromPersistence(entity);
    }

    @Override
    public void delete(TimeBlock timeBlock) {
        timeBlockPersistenceRepository.delete("organizationId = ?1 and id = ?2", timeBlock.getOrganizationId().value(),
                timeBlock.getId().value());
    }
}
