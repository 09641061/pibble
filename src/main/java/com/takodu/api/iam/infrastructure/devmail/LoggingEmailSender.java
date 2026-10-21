package com.takodu.api.iam.infrastructure.devmail;

import com.takodu.api.iam.application.internal.outboundservices.email.EmailSender;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.iam.domain.model.valueobjects.LoginCodeId;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dev mode only: prints the sign-in code to the console so a developer can sign in without Resend. The
 * {@code prod} build does not contain this bean, so codes are never logged in production.
 */
@ApplicationScoped
@IfBuildProfile("dev")
public class LoggingEmailSender implements EmailSender {

    @Override
    public void sendLoginCode(Email to, String code, LoginCodeId loginCodeId) {
        Log.infof("Dev sign-in code for %s: %s", to.value(), code);
    }
}
