package com.takodu.api.iam.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.iam.domain.model.aggregates.Session;
import com.takodu.api.iam.domain.model.valueobjects.SessionId;
import com.takodu.api.iam.infrastructure.persistence.jpa.entities.SessionPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

public final class SessionPersistenceAssembler {
    private SessionPersistenceAssembler() { }

    public static Session toDomainFromPersistence(SessionPersistenceEntity entity) {
        return new Session(new SessionId(entity.getId()), new UserId(entity.getUserId()), entity.getRefreshTokenHash(),
                entity.getPreviousRefreshTokenHash(), entity.getRotatedAt(), entity.getExpiresAt(),
                entity.getRevokedAt(), entity.getLastUsedAt());
    }

    public static SessionPersistenceEntity toPersistenceFromDomain(Session session) {
        var entity = new SessionPersistenceEntity();
        entity.setId(session.getId() == null ? null : session.getId().value());
        entity.setUserId(session.getUserId().value());
        entity.setRefreshTokenHash(session.getRefreshTokenHash());
        entity.setPreviousRefreshTokenHash(session.getPreviousRefreshTokenHash());
        entity.setRotatedAt(session.getRotatedAt());
        entity.setExpiresAt(session.getExpiresAt());
        entity.setRevokedAt(session.getRevokedAt());
        entity.setLastUsedAt(session.getLastUsedAt());
        return entity;
    }
}
