package com.takodu.api.iam.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.AccessDeniedException;

/**
 * I6: a disabled user cannot sign in or refresh a session.
 */
public class UserDisabledException extends AccessDeniedException {

    public UserDisabledException() {
        super("sign in: the user is disabled");
    }
}
