package com.takodu.api.organization.application.internal.commandservices;

import com.takodu.api.organization.application.commandservices.EstablishmentCommandService;
import com.takodu.api.organization.domain.exceptions.EstablishmentArchivedException;
import com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.organization.domain.exceptions.OrganizationNotFoundException;
import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.domain.model.commands.ArchiveEstablishmentCommand;
import com.takodu.api.organization.domain.model.commands.DescribeEstablishmentCommand;
import com.takodu.api.organization.domain.model.commands.OpenEstablishmentCommand;
import com.takodu.api.organization.domain.repositories.EstablishmentRepository;
import com.takodu.api.organization.domain.repositories.OrganizationRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;

@ApplicationScoped
public class EstablishmentCommandServiceImpl implements EstablishmentCommandService {
    private final EstablishmentRepository establishmentRepository;
    private final OrganizationRepository organizationRepository;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public EstablishmentCommandServiceImpl(EstablishmentRepository establishmentRepository,
                                           OrganizationRepository organizationRepository, AccessPolicy accessPolicy,
                                           Clock clock) {
        this.establishmentRepository = establishmentRepository;
        this.organizationRepository = organizationRepository;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    /** The organization row stays locked until commit, so two requests cannot both take the last free place. */
    @Override
    @Transactional
    public Establishment handle(OpenEstablishmentCommand command) {
        accessPolicy.require(Action.MANAGE_ESTABLISHMENTS);
        var earlier = establishmentRepository.findByIdempotencyKey(command.organizationId(), command.idempotencyKey());
        if (earlier.isPresent()) return earlier.get();
        var organization = organizationRepository.findByIdForUpdate(command.organizationId())
                .orElseThrow(() -> new OrganizationNotFoundException(command.organizationId()));
        organization.requireActivePlan(clock.instant());
        organization.ensureCanAddEstablishment(establishmentRepository.countActive(command.organizationId()));
        return establishmentRepository.save(Establishment.open(command.organizationId(), command.name(),
                command.address(), command.timeZone(), command.idempotencyKey()));
    }

    @Override
    @Transactional
    public Establishment handle(DescribeEstablishmentCommand command) {
        accessPolicy.require(Action.MANAGE_ESTABLISHMENTS);
        requireActivePlan(command.organizationId());
        var establishment = load(command.organizationId(), command.establishmentId());
        if (!establishment.isActive()) throw new EstablishmentArchivedException(establishment.getId());
        establishment.describe(command.name(), command.address(), command.timeZone());
        return establishmentRepository.save(establishment);
    }

    @Override
    @Transactional
    public Establishment handle(ArchiveEstablishmentCommand command) {
        accessPolicy.require(Action.MANAGE_ESTABLISHMENTS);
        var establishment = load(command.organizationId(), command.establishmentId());
        establishment.archive(clock.instant());
        return establishmentRepository.save(establishment);
    }

    private Establishment load(OrganizationId organizationId, EstablishmentId establishmentId) {
        return establishmentRepository.findById(organizationId, establishmentId)
                .orElseThrow(() -> new EstablishmentNotFoundException(establishmentId));
    }

    private void requireActivePlan(OrganizationId organizationId) {
        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId))
                .requireActivePlan(clock.instant());
    }
}
