package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.exceptions.InvitationAlreadyAcceptedException;
import com.takodu.api.organization.domain.exceptions.InvitationEmailMismatchException;
import com.takodu.api.organization.domain.exceptions.InvitationExpiredException;
import com.takodu.api.organization.domain.exceptions.InvitationNotPendingException;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
