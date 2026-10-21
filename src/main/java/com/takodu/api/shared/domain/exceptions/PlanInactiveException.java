package com.takodu.api.shared.domain.exceptions;

/**
 * The organization's plan is not active, so the operation is refused (rule O6). Maps to HTTP 402.
 */
public class PlanInactiveException extends DomainException {

    /**
     * @param organizationId the organization whose {@code plan_active_until} is in the past
     */
    public PlanInactiveException(Object organizationId) {
        super("PLAN_INACTIVE", "The plan of organization %s is not active".formatted(organizationId));
    }
}
