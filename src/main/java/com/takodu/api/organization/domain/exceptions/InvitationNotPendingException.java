package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

/** Only a pending invitation can be renewed. */
public class InvitationNotPendingException extends BusinessRuleViolationException {
    public InvitationNotPendingException(MemberId memberId) {
        super("INVITATION_NOT_PENDING", "Member has no pending invitation: " + memberId.value());
    }
}
