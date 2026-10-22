package com.takodu.api.conversations.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.WhatsappLineId;
import com.takodu.api.conversations.infrastructure.persistence.jpa.entities.WhatsappLinePersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public final class WhatsappLinePersistenceAssembler {
    private WhatsappLinePersistenceAssembler() { }

    public static WhatsappLine toDomainFromPersistence(WhatsappLinePersistenceEntity entity) {
        return new WhatsappLine(new WhatsappLineId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new EstablishmentId(entity.getEstablishmentId()), entity.getWabaId(), entity.getPhoneNumberId(),
                entity.getDisplayPhone(), new EncryptedToken(entity.getAccessTokenEncrypted()), entity.getMode(),
                WhatsappLinePersistenceEntity.CONNECTED.equals(entity.getStatus()), entity.getConnectedAt(),
                entity.getLastIdempotencyKey() == null ? null : new IdempotencyKey(entity.getLastIdempotencyKey()));
    }

    public static WhatsappLinePersistenceEntity toPersistenceFromDomain(WhatsappLine line) {
        var entity = new WhatsappLinePersistenceEntity();
        entity.setId(line.getId() == null ? null : line.getId().value());
        entity.setOrganizationId(line.getOrganizationId().value());
        entity.setEstablishmentId(line.getEstablishmentId().value());
        entity.setWabaId(line.getWabaId());
        entity.setPhoneNumberId(line.getPhoneNumberId());
        entity.setDisplayPhone(line.getDisplayPhone());
        entity.setAccessTokenEncrypted(line.getAccessToken().value());
        entity.setMode(line.getMode());
        entity.setStatus(line.isConnected() ? WhatsappLinePersistenceEntity.CONNECTED
                : WhatsappLinePersistenceEntity.DISCONNECTED);
        entity.setConnectedAt(line.getConnectedAt());
        entity.setLastIdempotencyKey(line.getLastIdempotencyKey() == null ? null : line.getLastIdempotencyKey().value());
        return entity;
    }
}
