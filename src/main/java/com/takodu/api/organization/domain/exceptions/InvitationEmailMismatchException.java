package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.AccessDeniedException;

/** O10: accepting requires signing in with the invited email. */
public class InvitationEmailMismatchException extends AccessDeniedException {
    public InvitationEmailMismatchException() { super("accept this invitation: it was sent to another email"); }
}
