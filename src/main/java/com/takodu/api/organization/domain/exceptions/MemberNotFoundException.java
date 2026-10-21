package com.takodu.api.organization.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

public class MemberNotFoundException extends ResourceNotFoundException {
    public MemberNotFoundException(MemberId memberId) { super("Member", memberId.value()); }
}
