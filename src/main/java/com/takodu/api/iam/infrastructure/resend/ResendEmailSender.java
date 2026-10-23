package com.takodu.api.iam.infrastructure.resend;

import com.takodu.api.iam.application.internal.outboundservices.email.EmailSender;
import com.takodu.api.shared.domain.model.valueobjects.Email;
import com.takodu.api.iam.domain.model.valueobjects.LoginCodeId;
import com.takodu.api.shared.infrastructure.resend.ResendClient;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

/** Sends email through Resend in production. */
@ApplicationScoped
@IfBuildProfile("prod")
public class ResendEmailSender implements EmailSender {
    private final ResendClient resendClient;
    private final String apiKey;
    private final String from;

    public ResendEmailSender(@RestClient ResendClient resendClient,
                             @ConfigProperty(name = "takodu.email.resend-api-key") String apiKey,
                             @ConfigProperty(name = "takodu.email.from") String from) {
        this.resendClient = resendClient;
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void sendLoginCode(Email to, String code, LoginCodeId loginCodeId) {
        resendClient.send("Bearer " + apiKey, loginCodeId.value(), new ResendClient.SendEmailRequest(from,
                List.of(to.value()), "Tu código de Takodu: " + code,
                "Tu código para entrar a Takodu es " + code + ". Vence en 10 minutos.\n\n"
                        + "Si no lo pediste, ignora este correo."));
    }
}
