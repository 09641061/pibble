package com.takodu.api.organization.application.queryservices;

import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.organization.domain.model.queries.GetActiveMembershipQuery;
import com.takodu.api.organization.domain.model.queries.GetCurrentMembersQuery;

import java.util.List;
import java.util.Optional;

public interface MemberQueryService {
    /** @return the user's joined, not removed membership, empty when they may not act in the organization */
    Optional<Member> handle(GetActiveMembershipQuery query);

    /** @return active members and pending invitations, by display name */
    List<Member> handle(GetCurrentMembersQuery query);
}
