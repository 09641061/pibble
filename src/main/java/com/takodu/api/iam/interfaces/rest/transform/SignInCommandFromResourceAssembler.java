package com.takodu.api.iam.interfaces.rest.transform;

import com.takodu.api.iam.domain.model.commands.SignInCommand;
import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.interfaces.rest.resources.SignInResource;

public final class SignInCommandFromResourceAssembler {
    private SignInCommandFromResourceAssembler() { }

    public static SignInCommand toCommandFromResource(SignInResource resource) {
        return new SignInCommand(new Email(resource.email()), resource.code().strip());
    }
}
