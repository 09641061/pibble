package com.takodu.api.iam.domain.model.aggregates;

import com.takodu.api.iam.domain.exceptions.UserDisabledException;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * I1: a person who can sign in, identified by email. A user belongs to no organization; memberships live in
 * {@code organization}.
 */
@Getter
public class User {
    private final UserId id;
    private final Email email;
    private final String name;
    private final Instant disabledAt;
    private Instant lastLoginAt;
    private final boolean platformOperator;

    /**
     * I4: the first successful sign-in creates the user.
     *
     * @param email the verified email
     * @return a new user without a name
     */
    public static User register(Email email) {
        return new User(null, email, null, null, null, false);
    }

    /**
     * Rehydration constructor; only persistence assemblers call it.
     *
     * @param platformOperator whether the user is on the Takodu team, which only the database grants
     */
    public User(UserId id, Email email, String name, Instant disabledAt, Instant lastLoginAt, boolean platformOperator) {
        this.id = id;
        this.email = Objects.requireNonNull(email, "email is required");
        this.name = name;
        this.disabledAt = disabledAt;
        this.lastLoginAt = lastLoginAt;
        this.platformOperator = platformOperator;
    }

    /**
     * Records a successful sign-in.
     *
     * @param now the current instant
     * @throws UserDisabledException when the user is disabled (I6)
     */
    public void signIn(Instant now) {
        requireEnabled();
        this.lastLoginAt = now;
    }

    /**
     * @throws UserDisabledException when the user is disabled (I6)
     */
    public void requireEnabled() {
        if (disabledAt != null) throw new UserDisabledException();
    }

    /**
     * The internal API is for the Takodu team only (03 §2).
     *
     * @throws UserDisabledException when the user is disabled (I6)
     * @throws AccessDeniedException when the user is not a platform operator
     */
    public void requirePlatformOperator() {
        requireEnabled();
        if (!platformOperator) throw new AccessDeniedException("use the internal API");
    }
}
