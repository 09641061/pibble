package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ConflictException;

/** O3: a concurrent transfer gave the organization another owner first. */
public class OwnerAlreadyExistsException extends ConflictException {
    public OwnerAlreadyExistsException() {
        super("Owner", "The organization already has an owner; reload and try again");
    }
}
