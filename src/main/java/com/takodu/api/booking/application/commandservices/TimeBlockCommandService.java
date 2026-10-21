package com.takodu.api.booking.application.commandservices;

import com.takodu.api.booking.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.booking.domain.exceptions.MemberNotAtEstablishmentException;
import com.takodu.api.booking.domain.exceptions.MemberNotFoundException;
import com.takodu.api.booking.domain.exceptions.TimeBlockNotFoundException;
import com.takodu.api.booking.domain.model.aggregates.TimeBlock;
import com.takodu.api.booking.domain.model.commands.CreateTimeBlockCommand;
import com.takodu.api.booking.domain.model.commands.DeleteTimeBlockCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;

/**
 * B15: every member manages their own blocks; owners and managers manage everyone's and the whole establishment's.
 * Blocks only keep bookings out, so they can change while the plan is inactive.
 */
public interface TimeBlockCommandService {

    /**
     * @return the new block, or the one an earlier request with the same key created
     * @throws EstablishmentNotFoundException when the establishment is not in the organization
     * @throws AccessDeniedException when staff block someone else, the whole establishment, or an establishment they
     *         are not assigned to
     * @throws MemberNotFoundException when the blocked member is not an active member of the organization
     * @throws MemberNotAtEstablishmentException when the blocked member does not work there
     */
    TimeBlock handle(CreateTimeBlockCommand command);

    /**
     * @throws TimeBlockNotFoundException when the block is not in the organization
     * @throws AccessDeniedException when staff delete a block that is not their own
     */
    void handle(DeleteTimeBlockCommand command);
}
