package com.takodu.api.iam.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.AuthenticationFailedException;

/**
 * I2: the code is wrong, used, expired or out of attempts. The message does not say which, so it reveals
 * nothing to someone guessing.
 */
public class InvalidLoginCodeException extends AuthenticationFailedException {

    public InvalidLoginCodeException() {
        super("LOGIN_CODE_INVALID", "The code is invalid or expired; request a new one");
    }
}
