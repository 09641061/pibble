package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.aggregates.Organization;
import com.takodu.api.organization.domain.model.queries.GetJoinedOrganizationsQuery;
import com.takodu.api.organization.domain.model.queries.GetOrganizationAccountsQuery;
import com.takodu.api.organization.domain.model.queries.GetOrganizationByIdQuery;

import java.util.List;
import java.util.Optional;

public interface OrganizationQueryService {
    /** @return the organizations where the user is an active member, by name */
    List<JoinedOrganization> handle(GetJoinedOrganizationsQuery query);

    Optional<Organization> handle(GetOrganizationByIdQuery query);

    /** @return every organization, the plan ending soonest first */
    List<OrganizationAccount> handle(GetOrganizationAccountsQuery query);
}
