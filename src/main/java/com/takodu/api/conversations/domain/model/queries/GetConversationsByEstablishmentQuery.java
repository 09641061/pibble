package com.takodu.api.conversations.domain.model.queries;

import com.takodu.api.conversations.domain.model.valueobjects.PageCursor;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/**
 * The inbox of one establishment, newest activity first.
 *
 * @param after where the previous page ended; null for the first page
 */
public record GetConversationsByEstablishmentQuery(OrganizationId organizationId, EstablishmentId establishmentId,
                                                   PageCursor after, int size) {
    public static final int MAX_SIZE = 50;

    public GetConversationsByEstablishmentQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (establishmentId == null) throw new IllegalArgumentException("establishmentId is required");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size must be between 1 and 50");
    }
}
