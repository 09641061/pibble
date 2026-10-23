package com.takodu.api.organization.application.internal.queryservices;

import com.takodu.api.organization.application.queryservices.EstablishmentQueryService;
import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.domain.model.queries.GetActiveEstablishmentsQuery;
import com.takodu.api.organization.domain.model.queries.GetEstablishmentCalendarQuery;
import com.takodu.api.organization.domain.model.queries.GetEstablishmentByIdQuery;
import com.takodu.api.organization.domain.repositories.EstablishmentRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EstablishmentQueryServiceImpl implements EstablishmentQueryService {
    private final EstablishmentRepository establishmentRepository;
    private final TenantContext tenantContext;
    private final AccessPolicy accessPolicy;

    public EstablishmentQueryServiceImpl(EstablishmentRepository establishmentRepository, TenantContext tenantContext,
                                         AccessPolicy accessPolicy) {
        this.establishmentRepository = establishmentRepository;
        this.tenantContext = tenantContext;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public List<Establishment> handle(GetActiveEstablishmentsQuery query) {
        return establishmentRepository.findActive(query.organizationId()).stream()
                .filter(establishment -> tenantContext.canSee(establishment.getId()))
                .toList();
    }

    @Override
    public Optional<Establishment> handle(GetEstablishmentByIdQuery query) {
        var establishment = establishmentRepository.findById(query.organizationId(), query.establishmentId());
        establishment.ifPresent(found -> accessPolicy.requireEstablishment(found.getId()));
        return establishment;
    }

    @Override
    public Optional<Establishment> handle(GetEstablishmentCalendarQuery query) {
        return establishmentRepository.findById(query.organizationId(), query.establishmentId());
    }
}
