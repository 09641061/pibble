package com.takodu.api.organization.application.commandservices;

import com.takodu.api.organization.domain.exceptions.EstablishmentArchivedException;
import com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.organization.domain.exceptions.PlanLimitReachedException;
import com.takodu.api.organization.domain.model.aggregates.Establishment;
import com.takodu.api.organization.domain.model.commands.ArchiveEstablishmentCommand;
import com.takodu.api.organization.domain.model.commands.DescribeEstablishmentCommand;
import com.takodu.api.organization.domain.model.commands.OpenEstablishmentCommand;
import com.takodu.api.organization.domain.model.commands.ReplaceOpeningHoursCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;

public interface EstablishmentCommandService {

    /**
     * Opens a branch, closed until its opening hours are set.
     *
     * @return the new establishment, or the one an earlier request with the same key opened
     * @throws AccessDeniedException when the caller is not the owner
     * @throws PlanInactiveException when the plan is not active (O6)
     * @throws PlanLimitReachedException when the plan allows no more active establishments (O4)
     */
    Establishment handle(OpenEstablishmentCommand command);

    /**
     * @throws AccessDeniedException when the caller is not the owner
     * @throws PlanInactiveException when the plan is not active (O6)
     * @throws EstablishmentNotFoundException when the establishment is not in the organization
     * @throws EstablishmentArchivedException when the establishment is archived
     */
    Establishment handle(DescribeEstablishmentCommand command);

    /**
     * O8: replaces the opening hours. Appointments already booked outside the new hours are kept.
     *
     * @throws AccessDeniedException when the caller is staff
     * @throws PlanInactiveException when the plan is not active (O6)
     * @throws EstablishmentNotFoundException when the establishment is not in the organization
     * @throws EstablishmentArchivedException when the establishment is archived
     */
    Establishment handle(ReplaceOpeningHoursCommand command);

    /**
     * Archives a branch and frees its place in the plan (O4). Works while the plan is inactive, so an owner can
     * always shrink to what they pay for.
     *
     * @throws AccessDeniedException when the caller is not the owner
     * @throws EstablishmentNotFoundException when the establishment is not in the organization
     */
    Establishment handle(ArchiveEstablishmentCommand command);
}
