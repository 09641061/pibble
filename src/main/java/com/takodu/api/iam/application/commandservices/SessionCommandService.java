package com.takodu.api.iam.application.commandservices;

import com.takodu.api.iam.domain.exceptions.InvalidLoginCodeException;
import com.takodu.api.iam.domain.exceptions.InvalidRefreshTokenException;
import com.takodu.api.iam.domain.exceptions.UserDisabledException;
import com.takodu.api.iam.domain.exceptions.SessionNotFoundException;
import com.takodu.api.iam.domain.model.aggregates.Session;
import com.takodu.api.iam.domain.model.commands.RefreshSessionCommand;
import com.takodu.api.iam.domain.model.commands.RevokeSessionCommand;
import com.takodu.api.iam.domain.model.commands.SignInCommand;

public interface SessionCommandService {

    /**
     * Any usable code of the email is accepted, so a code that arrived late still works; success consumes all
     * of them. The first sign-in of an email creates the user (I4).
     *
     * @param command the email and the typed code
     * @return the new session with its tokens
     * @throws InvalidLoginCodeException when no usable code matches (I2); the failed attempt is still recorded
     * @throws UserDisabledException when the user is disabled (I6)
     */
    SignedInSession handle(SignInCommand command);

    /**
     * Rotates the refresh token and issues a new access token for the same session (I5).
     *
     * @param command the refresh token the device holds
     * @return the session with its new tokens
     * @throws InvalidRefreshTokenException when the token is unknown, expired, revoked or reused late; a late
     *         reuse revokes the session and is committed
     * @throws UserDisabledException when the user is disabled (I6)
     */
    SignedInSession handle(RefreshSessionCommand command);

    /**
     * Signs a device out (I5). Revoking an already revoked session changes nothing.
     *
     * @param command the caller and the session to end
     * @return the revoked session
     * @throws SessionNotFoundException when the session does not belong to the caller
     */
    Session handle(RevokeSessionCommand command);
}
