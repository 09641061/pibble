package com.takodu.api.organization.application.commandservices;

import com.takodu.api.organization.application.queryservices.JoinedOrganization;
import com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.organization.domain.exceptions.InvitationAlreadyAcceptedException;
import com.takodu.api.organization.domain.exceptions.InvitationEmailMismatchException;
import com.takodu.api.organization.domain.exceptions.InvitationExpiredException;
import com.takodu.api.organization.domain.exceptions.InvitationNotFoundException;
import com.takodu.api.organization.domain.exceptions.InvitationNotPendingException;
import com.takodu.api.organization.domain.exceptions.MemberAlreadyExistsException;
import com.takodu.api.organization.domain.exceptions.MemberNotFoundException;
import com.takodu.api.organization.domain.exceptions.OwnerChangeNotAllowedException;
import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.commands.AcceptInvitationCommand;
import com.takodu.api.organization.domain.model.commands.InviteMemberCommand;
import com.takodu.api.organization.domain.model.commands.RenewInvitationCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;

public interface MemberCommandService {

    /**
     * O10: saves the invitation, then emails its link outside the transaction. Works while the plan is inactive,
     * so the team can always be managed.
     *
     * @return the pending member, or the one an earlier request with the same key invited (no second email)
     * @throws AccessDeniedException when the caller is staff
     * @throws OwnerChangeNotAllowedException when the role is owner (O12)
     * @throws MemberAlreadyExistsException when the email is already a member or invited (O11)
     * @throws EstablishmentNotFoundException when a staff establishment is not active in the organization
     */
    Member handle(InviteMemberCommand command);

    /**
     * O10: the caller joins the invitation's organization.
     *
     * @return the organization and the caller's membership; the same on a retry
     * @throws InvitationNotFoundException when the token matches no invitation or it was revoked
     * @throws InvitationExpiredException when the invitation is older than 7 days
     * @throws InvitationEmailMismatchException when the caller signed in with another email
     * @throws InvitationAlreadyAcceptedException when another user accepted it
     */
    JoinedOrganization handle(AcceptInvitationCommand command);

    /**
     * Emails a new link, valid another 7 days; the old link stops working.
     *
     * @return the pending member; a retry with the same key sends no second email
     * @throws AccessDeniedException when the caller is staff
     * @throws MemberNotFoundException when the member is not in the organization
     * @throws InvitationNotPendingException when the invitation was accepted
     */
    Member handle(RenewInvitationCommand command);
}
