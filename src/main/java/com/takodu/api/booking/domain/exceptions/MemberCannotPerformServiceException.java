package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

/** B2: the member does not perform the service, is not bookable, or does not work at its establishment. */
public class MemberCannotPerformServiceException extends BusinessRuleViolationException {
    public MemberCannotPerformServiceException(MemberId memberId) {
        super("MEMBER_CANNOT_PERFORM_SERVICE", "Member %s cannot perform this service".formatted(memberId.value()));
    }
}
