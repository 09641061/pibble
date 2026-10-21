package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

/** O12: ownership goes only to a member who joined; a pending invitation cannot own the organization. */
public class MemberNotActiveException extends BusinessRuleViolationException {
    public MemberNotActiveException(MemberId memberId) {
        super("MEMBER_NOT_ACTIVE", "Member has not joined yet: " + memberId.value());
    }
}
