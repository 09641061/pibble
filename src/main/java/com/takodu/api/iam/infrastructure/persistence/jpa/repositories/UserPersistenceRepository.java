package com.takodu.api.iam.infrastructure.persistence.jpa.repositories;

import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserPersistenceRepository implements PanacheRepositoryBase<UserPersistenceEntity, UUID> {
    public Optional<UserPersistenceEntity> findByEmail(Email email) {
        return find("email", email).firstResultOptional();
    }
}
