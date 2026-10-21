package com.takodu.api.iam.domain.model.commands;

import com.takodu.api.iam.domain.model.valueobjects.Email;

/**
 * Exchange an emailed code for a session.
 *
 * @param email the address the code was sent to
 * @param code the 6 digits the person typed
 */
public record SignInCommand(Email email, String code) {
    public SignInCommand {
        if (email == null) throw new IllegalArgumentException("email is required");
        if (code == null || !code.matches("\\d{6}")) throw new IllegalArgumentException("Code must be 6 digits");
    }
}
