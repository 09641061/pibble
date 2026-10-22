package com.takodu.api.conversations.application.queryservices;

import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.queries.GetWhatsappLineByEstablishmentQuery;

import java.util.Optional;

public interface WhatsappLineQueryService {
    /**
     * @return the connected line, else the last one; empty when the establishment never had one
     * @throws com.takodu.api.conversations.domain.exceptions.EstablishmentNotFoundException when the establishment is
     *         not in the organization
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when staff are not assigned to it
     */
    Optional<WhatsappLine> handle(GetWhatsappLineByEstablishmentQuery query);
}
