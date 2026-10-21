package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

/** O15: only bookable, active members who work at the service's establishment perform it. */
public class PerformerNotEligibleException extends BusinessRuleViolationException {
    public PerformerNotEligibleException(MemberId memberId) {
        super("PERFORMER_NOT_ELIGIBLE",
                "Member is not bookable, has not joined, or does not work at this establishment: " + memberId.value());
    }
}
