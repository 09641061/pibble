package com.takodu.api.organization.application.internal.commandservices;

import com.takodu.api.organization.application.commandservices.ServiceCommandService;
import com.takodu.api.organization.domain.exceptions.EstablishmentArchivedException;
import com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.organization.domain.exceptions.MemberNotFoundException;
import com.takodu.api.organization.domain.exceptions.OrganizationNotFoundException;
import com.takodu.api.organization.domain.exceptions.ServiceNotFoundException;
import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.aggregates.Service;
import com.takodu.api.organization.domain.model.commands.ActivateServiceCommand;
import com.takodu.api.organization.domain.model.commands.CreateServiceCommand;
import com.takodu.api.organization.domain.model.commands.DeactivateServiceCommand;
import com.takodu.api.organization.domain.model.commands.UpdateServiceCommand;
import com.takodu.api.organization.domain.repositories.EstablishmentRepository;
import com.takodu.api.organization.domain.repositories.MemberRepository;
import com.takodu.api.organization.domain.repositories.OrganizationRepository;
import com.takodu.api.organization.domain.repositories.ServiceRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.ServiceId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class ServiceCommandServiceImpl implements ServiceCommandService {
    private final ServiceRepository serviceRepository;
    private final EstablishmentRepository establishmentRepository;
    private final MemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public ServiceCommandServiceImpl(ServiceRepository serviceRepository, EstablishmentRepository establishmentRepository,
                                     MemberRepository memberRepository, OrganizationRepository organizationRepository,
                                     AccessPolicy accessPolicy, Clock clock) {
        this.serviceRepository = serviceRepository;
        this.establishmentRepository = establishmentRepository;
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Service handle(CreateServiceCommand command) {
        requireManager(command.organizationId());
        var earlier = serviceRepository.findByIdempotencyKey(command.organizationId(), command.idempotencyKey());
        if (earlier.isPresent()) return earlier.get();
        var establishment = establishmentRepository.findById(command.organizationId(), command.establishmentId())
                .orElseThrow(() -> new EstablishmentNotFoundException(command.establishmentId()));
        if (!establishment.isActive()) throw new EstablishmentArchivedException(establishment.getId());
        var service = Service.create(command.organizationId(), command.establishmentId(), command.name(),
                command.duration(), command.price(), command.instructions(), command.idempotencyKey());
        service.assignPerformers(members(command.organizationId(), command.performerIds()));
        return serviceRepository.save(service);
    }

    @Override
    @Transactional
    public Service handle(UpdateServiceCommand command) {
        requireManager(command.organizationId());
        var service = load(command.organizationId(), command.serviceId());
        service.describe(command.name(), command.duration(), command.price(), command.instructions());
        service.assignPerformers(members(command.organizationId(), command.performerIds()));
        return serviceRepository.save(service);
    }

    @Override
    @Transactional
    public Service handle(DeactivateServiceCommand command) {
        requireManager(command.organizationId());
        var service = load(command.organizationId(), command.serviceId());
        service.deactivate();
        return serviceRepository.save(service);
    }

    @Override
    @Transactional
    public Service handle(ActivateServiceCommand command) {
        requireManager(command.organizationId());
        var service = load(command.organizationId(), command.serviceId());
        service.activate();
        return serviceRepository.save(service);
    }

    private void requireManager(OrganizationId organizationId) {
        accessPolicy.require(Action.MANAGE_SERVICES_AND_HOURS);
        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId))
                .requireActivePlan(clock.instant());
    }

    private Service load(OrganizationId organizationId, ServiceId serviceId) {
        return serviceRepository.findById(organizationId, serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));
    }

    private List<Member> members(OrganizationId organizationId, Set<MemberId> memberIds) {
        return memberIds.stream()
                .map(id -> memberRepository.findCurrentById(organizationId, id)
                        .orElseThrow(() -> new MemberNotFoundException(id)))
                .toList();
    }
}
