package com.takodu.api.organization.application.commandservices;

import com.takodu.api.organization.domain.model.aggregates.Organization;
import com.takodu.api.organization.domain.model.commands.CreateOrganizationCommand;

public interface OrganizationCommandService {

    /**
     * O1: creates the organization, its first establishment and the caller as owner, in one transaction.
     *
     * @return the new organization, or the one an earlier request with the same key created
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key is still running, or the key belongs to another user's organization
     */
    Organization handle(CreateOrganizationCommand command);
}
