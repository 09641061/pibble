package com.takodu.api.organization.domain.model.queries;

import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

/** A current member: active, or invited and pending. */
public record GetMemberByIdQuery(OrganizationId organizationId, MemberId memberId) {
    public GetMemberByIdQuery {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        if (memberId == null) throw new IllegalArgumentException("memberId is required");
    }
}
