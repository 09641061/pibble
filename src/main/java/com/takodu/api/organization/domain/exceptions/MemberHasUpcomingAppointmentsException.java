package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

/** A member with upcoming appointments is not removed, so no booked customer is left without a person. */
public class MemberHasUpcomingAppointmentsException extends BusinessRuleViolationException {
    public MemberHasUpcomingAppointmentsException(MemberId memberId) {
        super("MEMBER_HAS_UPCOMING_APPOINTMENTS",
                "Reassign or cancel the member's upcoming appointments first: " + memberId.value());
    }
}
