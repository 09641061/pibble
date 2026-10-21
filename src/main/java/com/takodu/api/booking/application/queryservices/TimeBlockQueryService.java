package com.takodu.api.booking.application.queryservices;

import com.takodu.api.booking.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.booking.domain.model.aggregates.TimeBlock;
import com.takodu.api.booking.domain.model.queries.GetTimeBlocksByEstablishmentQuery;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;

import java.util.List;

public interface TimeBlockQueryService {
    /**
     * @return the blocks overlapping the window, earliest first
     * @throws EstablishmentNotFoundException when the establishment is not in the organization
     * @throws AccessDeniedException when the caller is staff not assigned to it (O13)
     */
    List<TimeBlock> handle(GetTimeBlocksByEstablishmentQuery query);
}
