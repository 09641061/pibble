package com.takodu.api.organization.infrastructure.resend;

import com.takodu.api.organization.application.internal.outboundservices.email.InvitationSender;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.shared.infrastructure.resend.ResendClient;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.UUID;

/** Sends invitations through Resend in production. */
@ApplicationScoped
@IfBuildProfile("prod")
public class ResendInvitationSender implements InvitationSender {
    private final ResendClient resendClient;
    private final String apiKey;
    private final String from;
    private final String appUrl;

    public ResendInvitationSender(@RestClient ResendClient resendClient,
                                  @ConfigProperty(name = "takodu.email.resend-api-key") String apiKey,
                                  @ConfigProperty(name = "takodu.email.from") String from,
                                  @ConfigProperty(name = "takodu.app-url") String appUrl) {
        this.resendClient = resendClient;
        this.apiKey = apiKey;
        this.from = from;
        this.appUrl = appUrl;
    }

    @Override
    public void sendInvitation(Email to, String organizationName, String invitedBy, String token, UUID idempotencyKey) {
        resendClient.send("Bearer " + apiKey, idempotencyKey, new ResendClient.SendEmailRequest(from,
                List.of(to.value()), invitedBy + " te invitó a " + organizationName + " en Takodu",
                invitedBy + " te invitó a unirte a " + organizationName + " en Takodu.\n\n"
                        + "Entra con este correo y acepta la invitación aquí: " + appUrl + "/invitations/" + token
                        + "\n\nEl enlace vence en 7 días."));
    }
}
