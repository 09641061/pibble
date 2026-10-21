package com.takodu.api.iam.infrastructure.persistence.jpa.adapters;

import com.takodu.api.iam.domain.model.aggregates.Session;
import com.takodu.api.iam.domain.model.valueobjects.SecretHash;
import com.takodu.api.iam.domain.repositories.SessionRepository;
import com.takodu.api.iam.infrastructure.persistence.jpa.assemblers.SessionPersistenceAssembler;
import com.takodu.api.iam.infrastructure.persistence.jpa.repositories.SessionPersistenceRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class SessionRepositoryImpl implements SessionRepository {
    private final SessionPersistenceRepository sessionPersistenceRepository;

    public SessionRepositoryImpl(SessionPersistenceRepository sessionPersistenceRepository) {
        this.sessionPersistenceRepository = sessionPersistenceRepository;
    }

    @Override
    public Optional<Session> findByRefreshTokenForUpdate(SecretHash refreshTokenHash) {
        return sessionPersistenceRepository.findByRefreshTokenForUpdate(refreshTokenHash)
                .map(SessionPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Session save(Session session) {
        var entity = SessionPersistenceAssembler.toPersistenceFromDomain(session);
        if (entity.getId() == null) {
            sessionPersistenceRepository.persist(entity);
        } else {
            entity = sessionPersistenceRepository.getEntityManager().merge(entity);
        }
        sessionPersistenceRepository.flush();
        return SessionPersistenceAssembler.toDomainFromPersistence(entity);
    }
}
