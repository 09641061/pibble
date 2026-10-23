package com.takodu.api.organization.infrastructure.persistence.jpa.adapters;

import com.takodu.api.organization.domain.model.aggregates.PlanChange;
import com.takodu.api.organization.domain.repositories.PlanChangeRepository;
import com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.PlanChangePersistenceAssembler;
import com.takodu.api.organization.infrastructure.persistence.jpa.repositories.PlanChangePersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PlanChangeRepositoryImpl implements PlanChangeRepository {
    private static final String IDEMPOTENCY_UNIQUE = "plan_changes_idempotency_uk";

    private final PlanChangePersistenceRepository planChangePersistenceRepository;

    public PlanChangeRepositoryImpl(PlanChangePersistenceRepository planChangePersistenceRepository) {
        this.planChangePersistenceRepository = planChangePersistenceRepository;
    }

    @Override
    public List<PlanChange> findByOrganization(OrganizationId organizationId) {
        return planChangePersistenceRepository.findByOrganization(organizationId.value()).stream()
                .map(PlanChangePersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public Optional<PlanChange> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey) {
        return planChangePersistenceRepository.findByIdempotencyKey(organizationId.value(), idempotencyKey.value())
                .map(PlanChangePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public PlanChange add(PlanChange planChange) {
        var entity = PlanChangePersistenceAssembler.toPersistenceFromDomain(planChange);
        try {
            planChangePersistenceRepository.persist(entity);
            planChangePersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (e.getConstraintName()) {
                case IDEMPOTENCY_UNIQUE -> throw new RequestInProgressException();
                case null, default -> throw e;
            }
        }
        return PlanChangePersistenceAssembler.toDomainFromPersistence(entity);
    }
}
