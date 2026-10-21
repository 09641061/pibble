package com.takodu.api.booking.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

/** The member is removed, still invited, or not in the organization. */
public class MemberNotFoundException extends ResourceNotFoundException {
    public MemberNotFoundException(MemberId memberId) { super("Member", memberId.value()); }
}
