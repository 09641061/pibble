package com.takodu.api.iam.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.TooManyRequestsException;

/**
 * I3: the email already received five codes in the last hour.
 */
public class LoginCodeLimitReachedException extends TooManyRequestsException {

    public LoginCodeLimitReachedException() {
        super("LOGIN_CODE_LIMIT", "Too many sign-in codes requested for this email; try again later");
    }
}
