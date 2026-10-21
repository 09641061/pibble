package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ConflictException;
import com.takodu.api.shared.domain.model.valueobjects.Email;

/** O11: an email has at most one active or pending membership per organization. */
public class MemberAlreadyExistsException extends ConflictException {
    public MemberAlreadyExistsException(Email email) {
        super("Member", "This email is already a member or invited: " + email.value());
    }
}
