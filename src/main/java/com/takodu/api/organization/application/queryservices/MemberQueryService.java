package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.queries.GetActiveMembershipQuery;

import java.util.Optional;

public interface MemberQueryService {
    /** @return the user's joined, not removed membership, empty when they may not act in the organization */
    Optional<Member> handle(GetActiveMembershipQuery query);
}
