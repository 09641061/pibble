package com.takodu.api.organization.application.commandservices;

import com.takodu.api.organization.domain.model.aggregates.PlanChange;
import com.takodu.api.organization.domain.model.commands.ChangePlanCommand;

/** Called only from the internal API, whose filter already admitted a platform operator. */
public interface PlanCommandService {

    /**
     * O5: sets the plan and its end, and appends the change to the history in the same transaction.
     *
     * @return the new history entry, or the one an earlier request with the same key wrote
     * @throws com.takodu.api.organization.domain.exceptions.OrganizationNotFoundException when no organization has
     *         the id
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    PlanChange handle(ChangePlanCommand command);
}
