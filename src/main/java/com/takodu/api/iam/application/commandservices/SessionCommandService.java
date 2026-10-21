package com.takodu.api.iam.application.commandservices;

import com.takodu.api.iam.domain.exceptions.InvalidLoginCodeException;
import com.takodu.api.iam.domain.exceptions.UserDisabledException;
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
}
