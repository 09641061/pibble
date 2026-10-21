package com.takodu.api.organization.infrastructure.persistence.jpa.adapters;

import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.repositories.MemberRepository;
import com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.MemberPersistenceAssembler;
import com.takodu.api.organization.infrastructure.persistence.jpa.repositories.MemberPersistenceRepository;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MemberRepositoryImpl implements MemberRepository {
    private final MemberPersistenceRepository memberPersistenceRepository;

    public MemberRepositoryImpl(MemberPersistenceRepository memberPersistenceRepository) {
        this.memberPersistenceRepository = memberPersistenceRepository;
    }

    @Override
    public Optional<Member> findActiveByUser(OrganizationId organizationId, UserId userId) {
        return memberPersistenceRepository.findActiveByUser(organizationId.value(), userId.value())
                .map(MemberPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Member> findActiveByUser(UserId userId) {
        return memberPersistenceRepository.findActiveByUser(userId.value()).stream()
                .map(MemberPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Member save(Member member) {
        var entity = MemberPersistenceAssembler.toPersistenceFromDomain(member);
        if (entity.getId() == null) {
            memberPersistenceRepository.persist(entity);
        } else {
            entity = memberPersistenceRepository.getEntityManager().merge(entity);
        }
        memberPersistenceRepository.flush();
        return MemberPersistenceAssembler.toDomainFromPersistence(entity);
    }
}
