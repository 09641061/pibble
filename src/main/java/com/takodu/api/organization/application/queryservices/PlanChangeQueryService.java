package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.queries.GetPlanChangesQuery;

import java.util.List;

/** Called only from the internal API. */
public interface PlanChangeQueryService {
    /**
     * @return the organization's plan history, newest first
     * @throws com.takodu.api.organization.domain.exceptions.OrganizationNotFoundException when no organization has
     *         the id
     */
    List<PlanChangeEntry> handle(GetPlanChangesQuery query);
}
