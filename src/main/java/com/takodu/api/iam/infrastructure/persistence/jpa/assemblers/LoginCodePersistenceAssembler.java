package com.takodu.api.iam.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.iam.domain.model.aggregates.LoginCode;
import com.takodu.api.iam.domain.model.valueobjects.LoginCodeId;
import com.takodu.api.iam.infrastructure.persistence.jpa.entities.LoginCodePersistenceEntity;

public final class LoginCodePersistenceAssembler {
    private LoginCodePersistenceAssembler() { }

    public static LoginCode toDomainFromPersistence(LoginCodePersistenceEntity entity) {
        return new LoginCode(new LoginCodeId(entity.getId()), entity.getEmail(), entity.getCodeHash(),
                entity.getAttempts(), entity.getExpiresAt(), entity.getConsumedAt());
    }

    public static LoginCodePersistenceEntity toPersistenceFromDomain(LoginCode loginCode) {
        var entity = new LoginCodePersistenceEntity();
        entity.setId(loginCode.getId() == null ? null : loginCode.getId().value());
        entity.setEmail(loginCode.getEmail());
        entity.setCodeHash(loginCode.getCodeHash());
        entity.setAttempts((short) loginCode.getAttempts());
        entity.setExpiresAt(loginCode.getExpiresAt());
        entity.setConsumedAt(loginCode.getConsumedAt());
        return entity;
    }
}
