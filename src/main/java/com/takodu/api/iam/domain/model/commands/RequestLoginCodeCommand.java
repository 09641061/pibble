package com.takodu.api.iam.domain.model.commands;

import com.takodu.api.shared.domain.model.valueobjects.Email;

/**
 * Email a new sign-in code to this address.
 *
 * @param email where the code is sent
 */
public record RequestLoginCodeCommand(Email email) {
    public RequestLoginCodeCommand {
        if (email == null) throw new IllegalArgumentException("email is required");
    }
}
