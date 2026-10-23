package com.takodu.api.iam.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.iam.domain.model.aggregates.User;
import com.takodu.api.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.UserId;

public final class UserPersistenceAssembler {
    private UserPersistenceAssembler() { }

    public static User toDomainFromPersistence(UserPersistenceEntity entity) {
        return new User(new UserId(entity.getId()), entity.getEmail(), entity.getName(), entity.getDisabledAt(),
                entity.getLastLoginAt(), entity.isPlatformOperator());
    }

    public static UserPersistenceEntity toPersistenceFromDomain(User user) {
        var entity = new UserPersistenceEntity();
        entity.setId(user.getId() == null ? null : user.getId().value());
        entity.setEmail(user.getEmail());
        entity.setName(user.getName());
        entity.setDisabledAt(user.getDisabledAt());
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setPlatformOperator(user.isPlatformOperator());
        return entity;
    }
}
