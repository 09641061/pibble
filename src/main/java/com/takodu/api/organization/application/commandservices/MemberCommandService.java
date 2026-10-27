package com.takodu.api.organization.application.commandservices;

import com.takodu.api.organization.application.queryservices.JoinedOrganization;
import com.takodu.api.organization.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.organization.domain.exceptions.InvitationAlreadyAcceptedException;
import com.takodu.api.organization.domain.exceptions.InvitationEmailMismatchException;
import com.takodu.api.organization.domain.exceptions.InvitationExpiredException;
import com.takodu.api.organization.domain.exceptions.InvitationNotFoundException;
import com.takodu.api.organization.domain.exceptions.InvitationNotPendingException;
import com.takodu.api.organization.domain.exceptions.MemberAlreadyExistsException;
import com.takodu.api.organization.domain.exceptions.MemberHasUpcomingAppointmentsException;
import com.takodu.api.organization.domain.exceptions.MemberNotActiveException;
import com.takodu.api.organization.domain.exceptions.MemberNotFoundException;
import com.takodu.api.organization.domain.exceptions.OwnerAlreadyExistsException;
import com.takodu.api.organization.domain.exceptions.OwnerChangeNotAllowedException;
import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.commands.AcceptInvitationCommand;
import com.takodu.api.organization.domain.model.commands.InviteMemberCommand;
import com.takodu.api.organization.domain.model.commands.RemoveMemberCommand;
import com.takodu.api.organization.domain.model.commands.RenewInvitationCommand;
import com.takodu.api.organization.domain.model.commands.TransferOwnershipCommand;
import com.takodu.api.organization.domain.model.commands.RenameOwnMembershipCommand;
import com.takodu.api.organization.domain.model.commands.UpdateMemberCommand;
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

    /**
     * Changes the name, bookability, role and staff establishments of an active or pending member.
     *
     * @throws AccessDeniedException when the caller is staff
     * @throws MemberNotFoundException when the member is not in the organization
     * @throws OwnerChangeNotAllowedException when the owner would change role or someone would become owner (O12)
     * @throws EstablishmentNotFoundException when a staff establishment is not active in the organization
     */
    Member handle(UpdateMemberCommand command);

    /**
     * Any member renames their own membership, whatever their role and while the plan is inactive.
     *
     * @throws MemberNotFoundException when the membership was removed meanwhile
     */
    Member handle(RenameOwnMembershipCommand command);

    /**
     * Removes an active member or revokes a pending invitation; the person loses access on their next request.
     *
     * @throws AccessDeniedException when the caller is staff
     * @throws MemberNotFoundException when the member is not in the organization
     * @throws OwnerChangeNotAllowedException when the member is the owner (O12)
     * @throws MemberHasUpcomingAppointmentsException when the member still has appointments ahead
     */
    Member handle(RemoveMemberCommand command);

    /**
     * O12: the new owner gets the role and the caller becomes a manager, in one transaction.
     *
     * @return the new owner; a retry with the same key returns the current owner
     * @throws AccessDeniedException when the caller is not the owner, also when a concurrent transfer took it away
     * @throws MemberNotFoundException when the member is not in the organization
     * @throws MemberNotActiveException when the member has not joined
     * @throws OwnerAlreadyExistsException when a concurrent transfer finished first (O3)
     */
    Member handle(TransferOwnershipCommand command);
}
