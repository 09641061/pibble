package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.organization.domain.exceptions.InvitationAlreadyAcceptedException;
import com.takodu.api.organization.domain.exceptions.InvitationEmailMismatchException;
import com.takodu.api.organization.domain.exceptions.InvitationExpiredException;
import com.takodu.api.organization.domain.exceptions.InvitationNotFoundException;
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
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

/**
 * O9-O13: a person's membership in an organization, with one role. A pending invitation is a member without a
 * user.
 */
@Getter
public class Member {
    public static final Duration INVITE_TTL = Duration.ofDays(7);

    /**
     * The team's order, which breaks ties when no member was chosen for an appointment: members in the order they
     * joined, pending invitations last.
     */
    public static final Comparator<Member> TEAM_ORDER = Comparator
            .comparing(Member::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(member -> member.getId().value());

    private final MemberId id;
    private final OrganizationId organizationId;
    private UserId userId;
    private final Email email;
    private String displayName;
    private Role role;
    private boolean bookable;
    private SecretHash inviteTokenHash;
    private Instant inviteExpiresAt;
    private Instant joinedAt;
    private Instant removedAt;
    private Set<EstablishmentId> establishmentIds;
    private final IdempotencyKey idempotencyKey;
    private IdempotencyKey lastIdempotencyKey;

    /**
     * O1: the creator of an organization joins as its owner, bookable, because most owners also attend customers.
     */
    public static Member founder(OrganizationId organizationId, UserId userId, Email email, String displayName,
                                 Instant now) {
        return new Member(null, organizationId, Objects.requireNonNull(userId, "userId is required"), email,
                displayName, Role.OWNER, true, null, null, now, null, Set.of(), null, null);
    }

    /**
     * O10: an owner or manager invites a person by email; only the hash of the emailed token is kept.
     *
     * @param establishmentIds where a {@code staff} member works (O13), at least one; ignored for managers
     * @throws OwnerChangeNotAllowedException when the role is {@code owner} (O12)
     */
    public static Member invite(OrganizationId organizationId, Email email, String displayName, Role role,
                                Set<EstablishmentId> establishmentIds, boolean bookable, SecretHash tokenHash,
                                Instant now, IdempotencyKey idempotencyKey) {
        if (role == Role.OWNER) throw new OwnerChangeNotAllowedException();
        return new Member(null, organizationId, null, email, displayName, role, bookable,
                Objects.requireNonNull(tokenHash, "tokenHash is required"), now.plus(INVITE_TTL), null, null,
                assignments(role, establishmentIds), idempotencyKey, null);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Member(MemberId id, OrganizationId organizationId, UserId userId, Email email, String displayName,
                  Role role, boolean bookable, SecretHash inviteTokenHash, Instant inviteExpiresAt, Instant joinedAt,
                  Instant removedAt, Set<EstablishmentId> establishmentIds, IdempotencyKey idempotencyKey,
                  IdempotencyKey lastIdempotencyKey) {
        this.id = id;
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.userId = userId;
        this.email = Objects.requireNonNull(email, "email is required");
        this.displayName = requireDisplayName(displayName);
        this.role = Objects.requireNonNull(role, "role is required");
        this.bookable = bookable;
        this.inviteTokenHash = inviteTokenHash;
        this.inviteExpiresAt = inviteExpiresAt;
        this.joinedAt = joinedAt;
        this.removedAt = removedAt;
        this.establishmentIds = Set.copyOf(establishmentIds);
        this.idempotencyKey = idempotencyKey;
        this.lastIdempotencyKey = lastIdempotencyKey;
    }

    /** @return whether the person joined and was not removed */
    public boolean isActive() {
        return userId != null && removedAt == null;
    }

    /** O13: whether the member has joined and works at the establishment; owners and managers work at all. */
    public boolean worksAt(EstablishmentId establishmentId) {
        return isActive() && (role.seesAllEstablishments() || establishmentIds.contains(establishmentId));
    }

    /** O15: whether the member may perform services of the establishment. */
    public boolean canPerformAt(EstablishmentId establishmentId) {
        return bookable && worksAt(establishmentId);
    }

    /** @return whether the invitation was neither accepted nor revoked */
    public boolean isPending() {
        return userId == null && removedAt == null;
    }

    /**
     * O10: the signed-in user joins. Accepting again as the same user changes nothing, so a retried request gets the
     * same membership.
     *
     * @param userEmail the email the user signed in with
     * @throws InvitationNotFoundException when the invitation was revoked
     * @throws InvitationAlreadyAcceptedException when another user accepted it
     * @throws InvitationExpiredException when it is older than {@link #INVITE_TTL}
     * @throws InvitationEmailMismatchException when the user signed in with another email
     */
    public void accept(UserId user, Email userEmail, Instant now) {
        if (removedAt != null) throw new InvitationNotFoundException();
        if (userId != null) {
            if (userId.equals(user)) return;
            throw new InvitationAlreadyAcceptedException();
        }
        if (!now.isBefore(inviteExpiresAt)) throw new InvitationExpiredException();
        if (!email.equals(userEmail)) throw new InvitationEmailMismatchException();
        this.userId = user;
        this.joinedAt = now;
        this.inviteExpiresAt = null;
    }

    /**
     * Replaces the token with a new one, valid another {@link #INVITE_TTL}; the old link stops working.
     *
     * @throws InvitationNotPendingException when the invitation was accepted or revoked
     */
    public void renewInvitation(SecretHash tokenHash, Instant now, IdempotencyKey idempotencyKey) {
        if (!isPending()) throw new InvitationNotPendingException(id);
        this.inviteTokenHash = Objects.requireNonNull(tokenHash, "tokenHash is required");
        this.inviteExpiresAt = now.plus(INVITE_TTL);
        this.lastIdempotencyKey = idempotencyKey;
    }

    /** Changes how the team sees the member and whether they receive appointments. */
    public void describe(String displayName, boolean bookable) {
        rename(displayName);
        this.bookable = bookable;
    }

    /** Changes only the name the team sees; the member may do it themselves. */
    public void rename(String displayName) {
        this.displayName = requireDisplayName(displayName);
    }

    /**
     * O9, O13: changes the role and, for staff, the establishments they work at.
     *
     * @throws OwnerChangeNotAllowedException when the member is the owner or the new role is owner (O12)
     */
    public void assign(Role newRole, Set<EstablishmentId> establishmentIds) {
        if (role == Role.OWNER || newRole == Role.OWNER) throw new OwnerChangeNotAllowedException();
        this.establishmentIds = assignments(newRole, establishmentIds);
        this.role = newRole;
    }

    /**
     * Removes an active member or revokes a pending invitation. The row stays for history (soft delete).
     *
     * @param hasUpcomingAppointments whether booking holds appointments of this member that have not started
     * @throws OwnerChangeNotAllowedException when the member is the owner (O12)
     * @throws MemberHasUpcomingAppointmentsException when appointments of this member are still ahead
     */
    public void remove(Instant now, boolean hasUpcomingAppointments) {
        if (role == Role.OWNER) throw new OwnerChangeNotAllowedException();
        if (hasUpcomingAppointments) throw new MemberHasUpcomingAppointmentsException(id);
        this.removedAt = now;
        this.bookable = false;
    }

    /**
     * O12: one half of an ownership transfer; {@link #handOverOwnership()} on the current owner is the other.
     *
     * @throws MemberNotActiveException when the member has not joined
     */
    public void promoteToOwner() {
        if (!isActive()) throw new MemberNotActiveException(id);
        this.role = Role.OWNER;
        this.establishmentIds = Set.of();
    }

    /** O12: the former owner stays as a manager. */
    public void handOverOwnership() {
        if (role != Role.OWNER) throw new IllegalStateException("Only the owner hands over ownership");
        this.role = Role.MANAGER;
    }

    /** @return whether a transition with this key was already applied, so a retry returns the current state */
    public boolean alreadyApplied(IdempotencyKey idempotencyKey) {
        return idempotencyKey.equals(lastIdempotencyKey);
    }

    private static Set<EstablishmentId> assignments(Role role, Set<EstablishmentId> establishmentIds) {
        if (role.seesAllEstablishments()) return Set.of();
        if (establishmentIds == null || establishmentIds.isEmpty())
            throw new IllegalArgumentException("Staff must be assigned to at least one establishment");
        return Set.copyOf(establishmentIds);
    }

    private static String requireDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Display name must not be blank");
        return displayName.strip();
    }
}
