package com.takodu.api.organization.infrastructure.persistence.jpa.adapters;

import com.takodu.api.organization.domain.exceptions.MemberAlreadyExistsException;
import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.repositories.MemberRepository;
import com.takodu.api.organization.infrastructure.persistence.jpa.assemblers.MemberPersistenceAssembler;
import com.takodu.api.organization.infrastructure.persistence.jpa.repositories.MemberPersistenceRepository;
import com.takodu.api.shared.domain.exceptions.RequestInProgressException;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MemberRepositoryImpl implements MemberRepository {
    private static final String ACTIVE_EMAIL_UNIQUE = "members_active_email_uk";
    private static final String ACTIVE_USER_UNIQUE = "members_active_user_uk";
    private static final String IDEMPOTENCY_UNIQUE = "members_idempotency_uk";

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
    public Optional<Member> findCurrentById(OrganizationId organizationId, MemberId memberId) {
        return memberPersistenceRepository.findCurrentById(organizationId.value(), memberId.value())
                .map(MemberPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Member> findCurrent(OrganizationId organizationId) {
        return memberPersistenceRepository.findCurrent(organizationId.value()).stream()
                .map(MemberPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public boolean existsCurrentByEmail(OrganizationId organizationId, Email email) {
        return memberPersistenceRepository.existsCurrentByEmail(organizationId.value(), email);
    }

    @Override
    public Optional<Member> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey) {
        return memberPersistenceRepository.findByIdempotencyKey(organizationId.value(), idempotencyKey.value())
                .map(MemberPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Member> findByInviteTokenForUpdate(SecretHash tokenHash) {
        return memberPersistenceRepository.findByInviteTokenForUpdate(tokenHash)
                .map(MemberPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Member save(Member member) {
        var entity = MemberPersistenceAssembler.toPersistenceFromDomain(member);
        try {
            if (entity.getId() == null) {
                memberPersistenceRepository.persist(entity);
            } else {
                entity = memberPersistenceRepository.getEntityManager().merge(entity);
            }
            memberPersistenceRepository.flush();
        } catch (ConstraintViolationException e) {
            switch (e.getConstraintName()) {
                case ACTIVE_EMAIL_UNIQUE, ACTIVE_USER_UNIQUE -> throw new MemberAlreadyExistsException(member.getEmail());
                case IDEMPOTENCY_UNIQUE -> throw new RequestInProgressException();
                case null, default -> throw e;
            }
        }
        return MemberPersistenceAssembler.toDomainFromPersistence(entity);
    }
}
