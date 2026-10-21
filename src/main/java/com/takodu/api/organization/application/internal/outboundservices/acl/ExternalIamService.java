package com.takodu.api.organization.application.internal.outboundservices.acl;

import com.takodu.api.iam.interfaces.acl.IamContextFacade;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/** The only class in {@code organization} that talks to {@code iam}. */
@ApplicationScoped
public class ExternalIamService {
    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    /** @return the email the user signs in with, empty when the user does not exist */
    public Optional<Email> emailOf(UserId userId) {
        return iamContextFacade.fetchUserEmail(userId.value()).map(Email::new);
    }
}
