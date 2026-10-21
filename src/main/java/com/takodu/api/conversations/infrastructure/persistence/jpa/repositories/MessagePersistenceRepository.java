package com.takodu.api.conversations.infrastructure.persistence.jpa.repositories;

import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.MessagePersistenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MessagePersistenceRepository implements PanacheRepositoryBase<MessagePersistenceEntity, UUID> {
    public boolean existsByWamid(String wamid) {
        return count("wamid", wamid) > 0;
    }

    public Optional<MessagePersistenceEntity> lockByWamid(String wamid) {
        return find("wamid", wamid).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }
}
