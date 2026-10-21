package com.takodu.api.organization.domain.repositories;

import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import com.takodu.api.shared.domain.repositories.CrossTenant;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {
    /** @return the user's joined, not removed membership in the organization */
    Optional<Member> findActiveByUser(OrganizationId organizationId, UserId userId);

    @CrossTenant("a signed-in user listing their organizations before choosing one; each result names its organization")
    List<Member> findActiveByUser(UserId userId);

    Member save(Member member);
}
