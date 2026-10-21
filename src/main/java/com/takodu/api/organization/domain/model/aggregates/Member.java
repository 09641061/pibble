package com.takodu.api.organization.domain.model.aggregates;

import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import com.takodu.api.shared.domain.model.valueobjects.Role;
import com.takodu.api.shared.domain.model.valueobjects.SecretHash;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * O9-O13: a person's membership in an organization, with one role. A pending invitation is a member without a
 * user.
 */
@Getter
public class Member {
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

    /**
     * O1: the creator of an organization joins as its owner, bookable, because most owners also attend customers.
     */
    public static Member founder(OrganizationId organizationId, UserId userId, Email email, String displayName,
                                 Instant now) {
        return new Member(null, organizationId, Objects.requireNonNull(userId, "userId is required"), email,
                displayName, Role.OWNER, true, null, null, now, null, Set.of(), null);
    }

    /** Rehydration constructor; only persistence assemblers call it. */
    public Member(MemberId id, OrganizationId organizationId, UserId userId, Email email, String displayName,
                  Role role, boolean bookable, SecretHash inviteTokenHash, Instant inviteExpiresAt, Instant joinedAt,
                  Instant removedAt, Set<EstablishmentId> establishmentIds, IdempotencyKey idempotencyKey) {
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
    }

    /** @return whether the person joined and was not removed */
    public boolean isActive() {
        return userId != null && removedAt == null;
    }

    private static String requireDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Display name must not be blank");
        return displayName.strip();
    }
}
