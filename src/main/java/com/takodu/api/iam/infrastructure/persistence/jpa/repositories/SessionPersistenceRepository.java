package com.takodu.api.iam.infrastructure.persistence.jpa.repositories;

import com.takodu.api.iam.infrastructure.persistence.jpa.entities.SessionPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class SessionPersistenceRepository implements PanacheRepositoryBase<SessionPersistenceEntity, UUID> {
}
