package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ConflictException;

/** Another user already accepted this invitation. */
public class InvitationAlreadyAcceptedException extends ConflictException {
    public InvitationAlreadyAcceptedException() {
        super("Invitation", "The invitation was already accepted by another user");
    }
}
