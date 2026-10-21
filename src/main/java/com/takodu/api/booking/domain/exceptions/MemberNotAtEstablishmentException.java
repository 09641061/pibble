package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

/** O13: staff work only at the establishments they are assigned to. */
public class MemberNotAtEstablishmentException extends BusinessRuleViolationException {
    public MemberNotAtEstablishmentException(MemberId memberId) {
        super("MEMBER_NOT_AT_ESTABLISHMENT", "Member %s does not work at this establishment".formatted(memberId.value()));
    }
}
