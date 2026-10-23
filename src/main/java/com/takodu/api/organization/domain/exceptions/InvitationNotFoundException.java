package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;

/** The token matches no invitation, or the invitation was revoked. */
public class InvitationNotFoundException extends ResourceNotFoundException {
    public InvitationNotFoundException() { super("Invitation", "this token"); }
}
