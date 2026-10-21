package com.takodu.api.organization.application.commandservices;

import com.takodu.api.organization.domain.exceptions.EstablishmentArchivedException;
import com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.organization.domain.exceptions.MemberNotFoundException;
import com.takodu.api.organization.domain.exceptions.PerformerNotEligibleException;
import com.takodu.api.organization.domain.exceptions.ServiceNotFoundException;
import com.takodu.api.organization.domain.model.aggregates.Service;
import com.takodu.api.organization.domain.model.commands.ActivateServiceCommand;
import com.takodu.api.organization.domain.model.commands.CreateServiceCommand;
import com.takodu.api.organization.domain.model.commands.DeactivateServiceCommand;
import com.takodu.api.organization.domain.model.commands.UpdateServiceCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;

/** Every method needs an owner or manager and an active plan (O6). */
public interface ServiceCommandService {

    /**
     * @return the new service, or the one an earlier request with the same key created
     * @throws AccessDeniedException when the caller is staff
     * @throws PlanInactiveException when the plan is not active
     * @throws EstablishmentNotFoundException when the establishment is not in the organization
     * @throws EstablishmentArchivedException when the establishment is archived
     * @throws MemberNotFoundException when a performer is not in the organization
     * @throws PerformerNotEligibleException when a performer cannot work at the establishment (O15)
     */
    Service handle(CreateServiceCommand command);

    /**
     * @throws AccessDeniedException when the caller is staff
     * @throws PlanInactiveException when the plan is not active
     * @throws ServiceNotFoundException when the service is not in the organization
     * @throws MemberNotFoundException when a performer is not in the organization
     * @throws PerformerNotEligibleException when a performer cannot work at the establishment (O15)
     */
    Service handle(UpdateServiceCommand command);

    /**
     * @throws AccessDeniedException when the caller is staff
     * @throws PlanInactiveException when the plan is not active
     * @throws ServiceNotFoundException when the service is not in the organization
     */
    Service handle(DeactivateServiceCommand command);

    /**
     * @throws AccessDeniedException when the caller is staff
     * @throws PlanInactiveException when the plan is not active
     * @throws ServiceNotFoundException when the service is not in the organization
     */
    Service handle(ActivateServiceCommand command);
}
