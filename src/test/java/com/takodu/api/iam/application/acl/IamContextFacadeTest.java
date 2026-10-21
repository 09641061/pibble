package com.takodu.api.iam.application.acl;

import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import com.takodu.api.iam.interfaces.acl.IamContextFacade;
import com.takodu.api.iam.interfaces.rest.SignIn;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class IamContextFacadeTest {

    @Inject
    IamContextFacade iamContextFacade;

    @Inject
    RecordingEmailSender emailSender;

    @Test
    void returnsTheEmailOfASignedInUser() {
        var email = SignIn.newEmail();
        String userId = SignIn.as(email, emailSender).extract().path("user.id");

        assertEquals(Optional.of(email), iamContextFacade.fetchUserEmail(UUID.fromString(userId)));
    }

    @Test
    void unknownUserHasNoEmail() {
        assertEquals(Optional.empty(), iamContextFacade.fetchUserEmail(UUID.randomUUID()));
    }
}
