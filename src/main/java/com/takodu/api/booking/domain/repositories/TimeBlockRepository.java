package com.takodu.api.booking.domain.repositories;

import com.takodu.api.booking.domain.model.aggregates.TimeBlock;
import com.takodu.api.booking.domain.model.valueobjects.TimeBlockId;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.TimeRange;

import java.util.List;
import java.util.Optional;

public interface TimeBlockRepository {
    Optional<TimeBlock> findById(OrganizationId organizationId, TimeBlockId timeBlockId);

    Optional<TimeBlock> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey);

    /** @return the establishment's blocks, for one member or for everyone, that overlap the range; earliest first */
    List<TimeBlock> findOverlapping(OrganizationId organizationId, EstablishmentId establishmentId, TimeRange range);

    /**
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    TimeBlock save(TimeBlock timeBlock);

    void delete(TimeBlock timeBlock);
}
