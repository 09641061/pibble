package com.takodu.api.organization.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.infrastructure.persistence.jpa.entities.MemberPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

import java.util.HashSet;
import java.util.stream.Collectors;

import static com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.OrganizationPersistenceAssembler.key;
import static com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.OrganizationPersistenceAssembler.value;

public final class MemberPersistenceAssembler {
    private MemberPersistenceAssembler() { }

    public static Member toDomainFromPersistence(MemberPersistenceEntity entity) {
        return new Member(new MemberId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                entity.getUserId() == null ? null : new UserId(entity.getUserId()), entity.getEmail(),
                entity.getDisplayName(), entity.getRole(), entity.isBookable(), entity.getInviteTokenHash(),
                entity.getInviteExpiresAt(), entity.getJoinedAt(), entity.getRemovedAt(),
                entity.getEstablishmentIds().stream().map(EstablishmentId::new).collect(Collectors.toSet()),
                key(entity.getIdempotencyKey()), key(entity.getLastIdempotencyKey()));
    }

    public static MemberPersistenceEntity toPersistenceFromDomain(Member member) {
        var entity = new MemberPersistenceEntity();
        entity.setId(member.getId() == null ? null : member.getId().value());
        entity.setOrganizationId(member.getOrganizationId().value());
        entity.setUserId(member.getUserId() == null ? null : member.getUserId().value());
        entity.setEmail(member.getEmail());
        entity.setDisplayName(member.getDisplayName());
        entity.setRole(member.getRole());
        entity.setBookable(member.isBookable());
        entity.setInviteTokenHash(member.getInviteTokenHash());
        entity.setInviteExpiresAt(member.getInviteExpiresAt());
        entity.setJoinedAt(member.getJoinedAt());
        entity.setRemovedAt(member.getRemovedAt());
        entity.setEstablishmentIds(member.getEstablishmentIds().stream().map(EstablishmentId::value)
                .collect(Collectors.toCollection(HashSet::new)));
        entity.setIdempotencyKey(value(member.getIdempotencyKey()));
        entity.setLastIdempotencyKey(value(member.getLastIdempotencyKey()));
        return entity;
    }
}
