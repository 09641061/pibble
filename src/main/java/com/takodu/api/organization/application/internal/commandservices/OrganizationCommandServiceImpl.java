package com.takodu.api.organization.application.internal.commandservices;

import com.takodu.api.organization.application.commandservices.OrganizationCommandService;
import com.takodu.api.organization.application.internal.outboundservices.acl.ExternalIamService;
import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.aggregates.Organization;
import com.takodu.api.organization.domain.exceptions.OrganizationNotFoundException;
import com.takodu.api.organization.domain.model.commands.CreateOrganizationCommand;
import com.takodu.api.organization.domain.model.commands.RenameOrganizationCommand;
import com.takodu.api.organization.domain.repositories.EstablishmentRepository;
import com.takodu.api.organization.domain.repositories.MemberRepository;
import com.takodu.api.organization.domain.repositories.OrganizationRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.Duration;

@ApplicationScoped
public class OrganizationCommandServiceImpl implements OrganizationCommandService {
    private final OrganizationRepository organizationRepository;
    private final EstablishmentRepository establishmentRepository;
    private final MemberRepository memberRepository;
    private final ExternalIamService externalIamService;
    private final AccessPolicy accessPolicy;
    private final Clock clock;
    private final Duration trial;

    public OrganizationCommandServiceImpl(OrganizationRepository organizationRepository,
                                          EstablishmentRepository establishmentRepository,
                                          MemberRepository memberRepository, ExternalIamService externalIamService,
                                          AccessPolicy accessPolicy, Clock clock,
                                          @ConfigProperty(name = "takodu.organization.trial-days") int trialDays) {
        this.organizationRepository = organizationRepository;
        this.establishmentRepository = establishmentRepository;
        this.memberRepository = memberRepository;
        this.externalIamService = externalIamService;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
        this.trial = Duration.ofDays(trialDays);
    }

    @Override
    @Transactional
    public Organization handle(CreateOrganizationCommand command) {
        var earlier = organizationRepository.findByIdempotencyKey(command.idempotencyKey());
        if (earlier.isPresent()) {
            var founder = memberRepository.findActiveByUser(earlier.get().getId(), command.creatorId());
            if (founder.filter(member -> member.getRole() == Role.OWNER).isEmpty()) throw new RequestInProgressException();
            return earlier.get();
        }
        var now = clock.instant();
        var email = externalIamService.emailOf(command.creatorId())
                .orElseThrow(() -> new IllegalStateException("The signed-in user does not exist"));
        var organization = organizationRepository.save(
                Organization.create(command.name(), now.plus(trial), command.idempotencyKey()));
        establishmentRepository.save(Establishment.open(organization.getId(), command.establishmentName(),
                command.address(), command.timeZone(), null));
        memberRepository.save(Member.founder(organization.getId(), command.creatorId(), email,
                command.ownerDisplayName(), now));
        return organization;
    }

    @Override
    @Transactional
    public Organization handle(RenameOrganizationCommand command) {
        accessPolicy.require(Action.MANAGE_ORGANIZATION);
        var organization = organizationRepository.findById(command.organizationId())
                .orElseThrow(() -> new OrganizationNotFoundException(command.organizationId()));
        organization.requireActivePlan(clock.instant());
        organization.rename(command.name());
        return organizationRepository.save(organization);
    }
}
