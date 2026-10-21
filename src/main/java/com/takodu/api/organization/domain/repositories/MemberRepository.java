package com.takodu.api.organization.domain.repositories;

import com.takodu.api.organization.domain.model.aggregates.Member;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import com.takodu.api.shared.domain.repositories.CrossTenant;

import java.util.List;
import java.util.Optional;

/** "Current" members are active or pending: not removed and not revoked. */
public interface MemberRepository {
    /** @return the user's joined, not removed membership in the organization */
    Optional<Member> findActiveByUser(OrganizationId organizationId, UserId userId);

    @CrossTenant("a signed-in user listing their organizations before choosing one; each result names its organization")
    List<Member> findActiveByUser(UserId userId);

    Optional<Member> findCurrentById(OrganizationId organizationId, MemberId memberId);

    /** @return active members and pending invitations, by display name */
    List<Member> findCurrent(OrganizationId organizationId);

    boolean existsCurrentByEmail(OrganizationId organizationId, Email email);

    Optional<Member> findByIdempotencyKey(OrganizationId organizationId, IdempotencyKey idempotencyKey);

    @CrossTenant("accepting an invitation: the user is not a member yet; the row names its organization")
    Optional<Member> findByInviteTokenForUpdate(SecretHash tokenHash);

    /**
     * @throws com.takodu.api.organization.domain.exceptions.MemberAlreadyExistsException when the email or the user
     *         already has a current membership in the organization (O11)
     * @throws com.takodu.api.shared.domain.exceptions.RequestInProgressException when a concurrent request with the
     *         same key inserted first
     */
    Member save(Member member);
}
