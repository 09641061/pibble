package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.queries.GetJoinedOrganizationsQuery;

import java.util.List;

public interface OrganizationQueryService {
    /** @return the organizations where the user is an active member, by name */
    List<JoinedOrganization> handle(GetJoinedOrganizationsQuery query);
}
