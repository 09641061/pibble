package com.takodu.api.organization.application.commandservices;

import com.takodu.api.organization.domain.model.aggregates.Organization;
import com.takodu.api.organization.domain.model.commands.CreateOrganizationCommand;
import com.takodu.api.organization.domain.model.commands.RenameOrganizationCommand;

public interface OrganizationCommandService {

    /**
     * O1: creates the organization, its first establishment and the caller as owner, in one transaction.
     *
     * @return the new organization, or the one an earlier request with the same key created
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key is still running, or the key belongs to another user's organization
     */
    Organization handle(CreateOrganizationCommand command);

    /**
     * @return the renamed organization
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when the caller is not the owner
     * @throws com.takodu.api.shared.domain.exceptions.PlanInactiveException when the plan is not active (O6)
     */
    Organization handle(RenameOrganizationCommand command);
}
