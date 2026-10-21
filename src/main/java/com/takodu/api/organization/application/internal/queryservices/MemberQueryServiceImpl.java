package com.takodu.api.organization.application.internal.queryservices;

import com.takodu.api.organization.application.queryservices.MemberQueryService;
import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.queries.GetActiveMembershipQuery;
import com.takodu.api.organization.domain.model.queries.GetCurrentMembersQuery;
import com.takodu.api.organization.domain.repositories.MemberRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MemberQueryServiceImpl implements MemberQueryService {
    private final MemberRepository memberRepository;

    public MemberQueryServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Optional<Member> handle(GetActiveMembershipQuery query) {
        return memberRepository.findActiveByUser(query.organizationId(), query.userId());
    }

    @Override
    public List<Member> handle(GetCurrentMembersQuery query) {
        return memberRepository.findCurrent(query.organizationId());
    }
}
