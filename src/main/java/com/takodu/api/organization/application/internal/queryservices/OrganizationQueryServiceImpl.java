package com.takodu.api.organization.application.internal.queryservices;

import com.takodu.api.organization.application.queryservices.JoinedOrganization;
import com.takodu.api.organization.application.queryservices.OrganizationAccount;
import com.takodu.api.organization.application.queryservices.OrganizationQueryService;
import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.aggregates.Organization;
import com.takodu.api.organization.domain.model.queries.GetJoinedOrganizationsQuery;
import com.takodu.api.organization.domain.model.queries.GetOrganizationAccountsQuery;
import com.takodu.api.organization.domain.model.queries.GetOrganizationByIdQuery;
import com.takodu.api.organization.domain.repositories.EstablishmentRepository;
import com.takodu.api.organization.domain.repositories.MemberRepository;
import com.takodu.api.organization.domain.repositories.OrganizationRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class OrganizationQueryServiceImpl implements OrganizationQueryService {
    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final EstablishmentRepository establishmentRepository;
    private final Clock clock;

    public OrganizationQueryServiceImpl(OrganizationRepository organizationRepository, MemberRepository memberRepository,
                                        EstablishmentRepository establishmentRepository, Clock clock) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.establishmentRepository = establishmentRepository;
        this.clock = clock;
    }

    /** A user belongs to a handful of organizations, so one lookup per membership is fine. */
    @Override
    public List<JoinedOrganization> handle(GetJoinedOrganizationsQuery query) {
        return memberRepository.findActiveByUser(query.userId()).stream()
                .flatMap(member -> organizationRepository.findById(member.getOrganizationId()).stream()
                        .map(organization -> new JoinedOrganization(organization, member)))
                .sorted(Comparator.comparing(joined -> joined.organization().getName()))
                .toList();
    }

    @Override
    public Optional<Organization> handle(GetOrganizationByIdQuery query) {
        return organizationRepository.findById(query.organizationId());
    }

    /** Takodu serves a few dozen businesses, so two lookups per organization are fine. */
    @Override
    public List<OrganizationAccount> handle(GetOrganizationAccountsQuery query) {
        var now = clock.instant();
        return organizationRepository.findAll().stream()
                .map(organization -> new OrganizationAccount(organization,
                        memberRepository.findOwner(organization.getId()).map(Member::getEmail).orElse(null),
                        establishmentRepository.countActive(organization.getId()), organization.isPlanActive(now)))
                .sorted(Comparator.comparing(account -> account.organization().getPlanActiveUntil(),
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }
}
