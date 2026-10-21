package com.takodu.api.organization.application.internal.queryservices;

import com.takodu.api.organization.application.queryservices.BookableService;
import com.takodu.api.organization.application.queryservices.ServiceQueryService;
import com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.aggregates.Service;
import com.takodu.api.organization.domain.model.queries.GetBookableServiceQuery;
import com.takodu.api.organization.domain.model.queries.GetServiceByIdQuery;
import com.takodu.api.organization.domain.model.queries.GetServicesByEstablishmentQuery;
import com.takodu.api.organization.domain.repositories.EstablishmentRepository;
import com.takodu.api.organization.domain.repositories.MemberRepository;
import com.takodu.api.organization.domain.repositories.ServiceRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ServiceQueryServiceImpl implements ServiceQueryService {
    private final ServiceRepository serviceRepository;
    private final EstablishmentRepository establishmentRepository;
    private final MemberRepository memberRepository;
    private final AccessPolicy accessPolicy;

    public ServiceQueryServiceImpl(ServiceRepository serviceRepository, EstablishmentRepository establishmentRepository,
                                   MemberRepository memberRepository, AccessPolicy accessPolicy) {
        this.serviceRepository = serviceRepository;
        this.establishmentRepository = establishmentRepository;
        this.memberRepository = memberRepository;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public List<Service> handle(GetServicesByEstablishmentQuery query) {
        establishmentRepository.findById(query.organizationId(), query.establishmentId())
                .orElseThrow(() -> new EstablishmentNotFoundException(query.establishmentId()));
        accessPolicy.requireEstablishment(query.establishmentId());
        return serviceRepository.findByEstablishment(query.organizationId(), query.establishmentId());
    }

    @Override
    public Optional<Service> handle(GetServiceByIdQuery query) {
        var service = serviceRepository.findById(query.organizationId(), query.serviceId());
        service.ifPresent(found -> accessPolicy.requireEstablishment(found.getEstablishmentId()));
        return service;
    }

    /** A team is a few dozen members, so filtering the whole team is cheaper than a query per performer. */
    @Override
    public Optional<BookableService> handle(GetBookableServiceQuery query) {
        return serviceRepository.findById(query.organizationId(), query.serviceId()).map(service -> {
            var performers = memberRepository.findCurrent(query.organizationId()).stream()
                    .filter(member -> service.getPerformerIds().contains(member.getId()))
                    .filter(member -> member.canPerformAt(service.getEstablishmentId()))
                    .sorted(Member.TEAM_ORDER)
                    .toList();
            return new BookableService(service, performers);
        });
    }
}
