package com.takodu.api.iam.interfaces.rest.transform;

import com.takodu.api.iam.application.commandservices.SignedInSession;
import com.takodu.api.iam.interfaces.rest.resources.SessionTokensResource;
import com.takodu.api.iam.interfaces.rest.resources.UserResource;

public final class SessionTokensResourceFromEntityAssembler {
    private SessionTokensResourceFromEntityAssembler() { }

    public static SessionTokensResource toResourceFromEntity(SignedInSession signedIn) {
        var user = signedIn.user();
        return new SessionTokensResource(signedIn.session().getId().value(), signedIn.accessToken().value(),
                signedIn.accessToken().expiresAt(), signedIn.refreshToken(), signedIn.session().getExpiresAt(),
                new UserResource(user.getId().value(), user.getEmail().value(), user.getName()));
    }
}
