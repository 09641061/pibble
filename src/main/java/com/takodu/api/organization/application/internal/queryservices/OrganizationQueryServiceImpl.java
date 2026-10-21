package com.takodu.api.organization.application.internal.queryservices;

import com.takodu.api.organization.application.queryservices.JoinedOrganization;
import com.takodu.api.organization.application.queryservices.OrganizationQueryService;
import com.takodu.api.organization.domain.model.queries.GetJoinedOrganizationsQuery;
import com.takodu.api.organization.domain.repositories.MemberRepository;
import com.takodu.api.organization.domain.repositories.OrganizationRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class OrganizationQueryServiceImpl implements OrganizationQueryService {
    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;

    public OrganizationQueryServiceImpl(OrganizationRepository organizationRepository, MemberRepository memberRepository) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
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
}
