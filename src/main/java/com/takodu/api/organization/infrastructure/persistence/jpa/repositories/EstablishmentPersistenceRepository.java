package com.takodu.api.organization.infrastructure.persistence.jpa.repositories;

import com.takodu.api.organization.infrastructure.persistence.jpa.entities.EstablishmentPersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class EstablishmentPersistenceRepository implements PanacheRepositoryBase<EstablishmentPersistenceEntity, UUID> {
}
