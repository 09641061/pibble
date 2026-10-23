package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** O10: an invitation is valid 7 days; an owner or manager can renew it. */
public class InvitationExpiredException extends BusinessRuleViolationException {
    public InvitationExpiredException() { super("INVITATION_EXPIRED", "The invitation expired; ask for a new one"); }
}
