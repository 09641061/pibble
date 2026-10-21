package com.takodu.api.shared.domain.exceptions;

/**
 * The request is well formed but a business rule forbids it, such as an invalid state transition.
 * Maps to HTTP 422.
 */
public class BusinessRuleViolationException extends DomainException {

    /**
     * @param rule rule name, upper snake case, used as the error code
     * @param message why the rule rejects the operation
     */
    public BusinessRuleViolationException(String rule, String message) {
        super(rule, message);
    }
}
