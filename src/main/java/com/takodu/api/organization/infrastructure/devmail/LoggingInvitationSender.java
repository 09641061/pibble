package com.takodu.api.organization.infrastructure.devmail;

import com.takodu.api.organization.application.internal.outboundservices.email.InvitationSender;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Dev mode only: prints the invitation token to the console so a developer can accept it without Resend. The
 * {@code prod} build does not contain this bean.
 */
@ApplicationScoped
@IfBuildProfile("dev")
public class LoggingInvitationSender implements InvitationSender {

    @Override
    public void sendInvitation(Email to, String organizationName, String invitedBy, String token, UUID idempotencyKey) {
        Log.infof("Dev invitation for %s to %s: token %s", to.value(), organizationName, token);
    }
}
