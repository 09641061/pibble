package com.takodu.api.booking.infrastructure.persistence.jpa.assemblers;

import com.takodu.api.booking.domain.model.aggregates.TimeBlock;
import com.takodu.api.booking.domain.model.valueobjects.TimeBlockId;
import com.takodu.api.booking.infrastructure.persistence.jpa.entities.TimeBlockPersistenceEntity;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;

public final class TimeBlockPersistenceAssembler {
    private TimeBlockPersistenceAssembler() { }

    public static TimeBlock toDomainFromPersistence(TimeBlockPersistenceEntity entity) {
        return new TimeBlock(new TimeBlockId(entity.getId()), new OrganizationId(entity.getOrganizationId()),
                new EstablishmentId(entity.getEstablishmentId()),
                entity.getStaffMemberId() == null ? null : new MemberId(entity.getStaffMemberId()),
                new TimeRange(entity.getStartsAt(), entity.getEndsAt()), entity.getReason(),
                entity.getIdempotencyKey() == null ? null : new IdempotencyKey(entity.getIdempotencyKey()));
    }

    public static TimeBlockPersistenceEntity toPersistenceFromDomain(TimeBlock timeBlock) {
        var entity = new TimeBlockPersistenceEntity();
        entity.setId(timeBlock.getId() == null ? null : timeBlock.getId().value());
        entity.setOrganizationId(timeBlock.getOrganizationId().value());
        entity.setEstablishmentId(timeBlock.getEstablishmentId().value());
        entity.setStaffMemberId(timeBlock.getStaffMemberId() == null ? null : timeBlock.getStaffMemberId().value());
        entity.setStartsAt(timeBlock.getRange().start());
        entity.setEndsAt(timeBlock.getRange().end());
        entity.setReason(timeBlock.getReason());
        entity.setIdempotencyKey(timeBlock.getIdempotencyKey() == null ? null : timeBlock.getIdempotencyKey().value());
        return entity;
    }
}
