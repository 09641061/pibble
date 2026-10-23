package com.takodu.api.assistant.application.queryservices;

import com.takodu.api.assistant.domain.model.queries.GetEstablishmentUsageQuery;
import com.takodu.api.assistant.domain.model.queries.GetOrganizationUsageQuery;
import com.takodu.api.assistant.domain.model.valueobjects.EstablishmentUsage;

import java.util.List;

/** A9: what the assistant used, in tokens and tool calls; prices stay outside the system. */
public interface AssistantUsageQueryService {

    /**
     * Works while the plan is inactive: it is a read (O6).
     *
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when the caller is staff
     * @throws com.takodu.api.assistant.domain.exceptions.EstablishmentNotFoundException when the establishment is
     *         not in the organization
     */
    EstablishmentUsage handle(GetEstablishmentUsageQuery query);

    /**
     * For the internal API, which admits only platform operators; it checks no {@code AccessPolicy}.
     *
     * @return each establishment with answered turns in the period, by name
     * @throws com.takodu.api.assistant.domain.exceptions.OrganizationNotFoundException when no organization has the
     *         id
     */
    List<EstablishmentUsage> handle(GetOrganizationUsageQuery query);
}
