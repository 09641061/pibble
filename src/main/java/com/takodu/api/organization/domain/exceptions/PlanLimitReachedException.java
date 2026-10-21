package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.organization.domain.model.valueobjects.Plan;
import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** O4: the plan allows no more active establishments. */
public class PlanLimitReachedException extends BusinessRuleViolationException {
    public PlanLimitReachedException(Plan plan) {
        super("PLAN_LIMIT_REACHED", "The %s plan allows no more active establishments; archive one or upgrade to max"
                .formatted(plan.name().toLowerCase()));
    }
}
