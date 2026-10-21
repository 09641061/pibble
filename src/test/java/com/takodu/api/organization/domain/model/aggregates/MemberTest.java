package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.exceptions.InvitationAlreadyAcceptedException;
import com.takodu.api.organization.domain.exceptions.InvitationEmailMismatchException;
import com.takodu.api.organization.domain.exceptions.InvitationExpiredException;
import com.takodu.api.organization.domain.exceptions.InvitationNotPendingException;
import com.takodu.api.organization.domain.exceptions.MemberHasUpcomingAppointmentsException;
import com.takodu.api.organization.domain.exceptions.MemberNotActiveException;
import com.takodu.api.organization.domain.exceptions.OwnerChangeNotAllowedException;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.Role;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTest {
    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
    private static final Email EMAIL = new Email("lucia@example.com");

    private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
    private final EstablishmentId branch = new EstablishmentId(UUID.randomUUID());
    private final UserId lucia = new UserId(UUID.randomUUID());

    @Test
    void ownersAreNotInvited() {
        assertThrows(OwnerChangeNotAllowedException.class, () -> invite(Role.OWNER, Set.of()));
    }

    @Test
    void staffNeedAnEstablishment() {
        assertThrows(IllegalArgumentException.class, () -> invite(Role.STAFF, Set.of()));
    }

    @Test
    void managersSeeEveryEstablishmentSoTheirAssignmentsAreDropped() {
        assertTrue(invite(Role.MANAGER, Set.of(branch)).getEstablishmentIds().isEmpty());
    }

    @Test
    void invitedUserJoins() {
        var member = invite(Role.STAFF, Set.of(branch));

        member.accept(lucia, EMAIL, NOW.plusSeconds(60));

        assertTrue(member.isActive());
        assertFalse(member.isPending());
    }

    @Test
    void acceptingAgainAsTheSameUserChangesNothing() {
        var member = invite(Role.STAFF, Set.of(branch));
        member.accept(lucia, EMAIL, NOW);

        member.accept(lucia, EMAIL, NOW.plusSeconds(60));

        assertEquals(NOW, member.getJoinedAt());
    }

    @Test
    void anotherUserCannotTakeAnAcceptedInvitation() {
        var member = invite(Role.STAFF, Set.of(branch));
        member.accept(lucia, EMAIL, NOW);

        assertThrows(InvitationAlreadyAcceptedException.class,
                () -> member.accept(new UserId(UUID.randomUUID()), EMAIL, NOW));
    }

    @Test
    void anotherEmailCannotAccept() {
        var member = invite(Role.STAFF, Set.of(branch));

        assertThrows(InvitationEmailMismatchException.class,
                () -> member.accept(lucia, new Email("other@example.com"), NOW));
    }

    @Test
    void invitationExpiresAfterSevenDays() {
        var member = invite(Role.STAFF, Set.of(branch));

        assertThrows(InvitationExpiredException.class, () -> member.accept(lucia, EMAIL, NOW.plus(Member.INVITE_TTL)));
    }

    @Test
    void renewalRestartsTheSevenDays() {
        var member = invite(Role.STAFF, Set.of(branch));
        var key = new IdempotencyKey(UUID.randomUUID());

        member.renewInvitation(SecretHash.of("new-token"), NOW.plus(Member.INVITE_TTL), key);

        assertTrue(member.alreadyApplied(key));
        member.accept(lucia, EMAIL, NOW.plus(Member.INVITE_TTL).plusSeconds(60));
        assertTrue(member.isActive());
    }

    @Test
    void acceptedInvitationIsNotRenewed() {
        var member = new Member(new MemberId(UUID.randomUUID()), organization, lucia, EMAIL, "Lucía", Role.STAFF, true,
                SecretHash.of("token"), null, NOW, null, Set.of(branch), null, null);

        assertThrows(InvitationNotPendingException.class,
                () -> member.renewInvitation(SecretHash.of("again"), NOW, new IdempotencyKey(UUID.randomUUID())));
    }

    private Member invite(Role role, Set<EstablishmentId> establishments) {
        return Member.invite(organization, EMAIL, "Lucía", role, establishments, true, SecretHash.of("token"), NOW,
                new IdempotencyKey(UUID.randomUUID()));
    }

    @Test
    void ownerKeepsTheRole() {
        var owner = founder();

        assertThrows(OwnerChangeNotAllowedException.class, () -> owner.assign(Role.MANAGER, Set.of()));
        assertThrows(OwnerChangeNotAllowedException.class, () -> owner.remove(NOW, false));
    }

    @Test
    void nobodyBecomesOwnerByAssignment() {
        var member = invite(Role.STAFF, Set.of(branch));

        assertThrows(OwnerChangeNotAllowedException.class, () -> member.assign(Role.OWNER, Set.of()));
    }

    @Test
    void promotedStaffNoLongerNeedsEstablishments() {
        var member = invite(Role.STAFF, Set.of(branch));

        member.assign(Role.MANAGER, Set.of(branch));

        assertEquals(Role.MANAGER, member.getRole());
        assertTrue(member.getEstablishmentIds().isEmpty());
    }

    @Test
    void memberWithUpcomingAppointmentsStays() {
        var member = stored(invite(Role.STAFF, Set.of(branch)));

        assertThrows(MemberHasUpcomingAppointmentsException.class, () -> member.remove(NOW, true));
    }

    @Test
    void removedMemberIsNoLongerBookable() {
        var member = invite(Role.STAFF, Set.of(branch));

        member.remove(NOW, false);

        assertNotNull(member.getRemovedAt());
        assertFalse(member.isBookable());
        assertFalse(member.isPending());
    }

    @Test
    void ownershipGoesOnlyToAMemberWhoJoined() {
        var member = stored(invite(Role.MANAGER, Set.of()));

        assertThrows(MemberNotActiveException.class, member::promoteToOwner);
    }

    @Test
    void transferSwapsTheRoles() {
        var owner = founder();
        var manager = invite(Role.MANAGER, Set.of());
        manager.accept(lucia, EMAIL, NOW);

        manager.promoteToOwner();
        owner.handOverOwnership();

        assertEquals(Role.OWNER, manager.getRole());
        assertEquals(Role.MANAGER, owner.getRole());
    }

    @Test
    void staffWorkOnlyWhereAssignedAndOnlyOnceJoined() {
        var member = invite(Role.STAFF, Set.of(branch));
        assertFalse(member.worksAt(branch));

        member.accept(lucia, EMAIL, NOW);

        assertTrue(member.worksAt(branch));
        assertFalse(member.worksAt(new EstablishmentId(UUID.randomUUID())));
        assertTrue(founder().worksAt(new EstablishmentId(UUID.randomUUID())));
    }

    @Test
    void teamOrderIsTheOrderMembersJoinedWithInvitationsLast() {
        var later = stored(invite(Role.STAFF, Set.of(branch)));
        later.accept(lucia, EMAIL, NOW.plusSeconds(60));
        var earlier = stored(founder());
        var pending = stored(invite(Role.MANAGER, Set.of()));

        var ordered = Stream.of(pending, later, earlier).sorted(Member.TEAM_ORDER).toList();

        assertEquals(List.of(earlier, later, pending), ordered);
    }

    /** The member as the repository returns it, with an id. */
    private static Member stored(Member member) {
        return new Member(new MemberId(UUID.randomUUID()), member.getOrganizationId(), member.getUserId(),
                member.getEmail(), member.getDisplayName(), member.getRole(), member.isBookable(),
                member.getInviteTokenHash(), member.getInviteExpiresAt(), member.getJoinedAt(), member.getRemovedAt(),
                member.getEstablishmentIds(), member.getIdempotencyKey(), member.getLastIdempotencyKey());
    }

    private Member founder() {
        return Member.founder(organization, new UserId(UUID.randomUUID()), new Email("pepe@example.com"), "Pepe", NOW);
    }
}
