package com.takodu.api.conversations.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

/** W7, O13: a conversation is assigned only to someone who can see its establishment's inbox. */
public class MemberNotAtEstablishmentException extends BusinessRuleViolationException {
    public MemberNotAtEstablishmentException(MemberId memberId) {
        super("MEMBER_NOT_AT_ESTABLISHMENT", "Member %s does not work at this establishment".formatted(memberId.value()));
    }
}
