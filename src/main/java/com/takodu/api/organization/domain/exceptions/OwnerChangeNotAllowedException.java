package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.BusinessRuleViolationException;

/** O12: the owner cannot be invited, removed or demoted; ownership moves only by transfer. */
public class OwnerChangeNotAllowedException extends BusinessRuleViolationException {
    public OwnerChangeNotAllowedException() {
        super("OWNER_CHANGE_NOT_ALLOWED",
                "The owner cannot be invited, removed or demoted; transfer ownership instead");
    }
}
