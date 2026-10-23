package com.takodu.api.shared.infrastructure.resend;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.UUID;

/** The Resend HTTP API, used by every context that emails people (https://resend.com/docs/api-reference/emails/send-email). */
@RegisterRestClient(configKey = "resend")
public interface ResendClient {

    @POST
    @Path("/emails")
    void send(@HeaderParam("Authorization") String authorization,
              @HeaderParam("Idempotency-Key") UUID idempotencyKey,
              SendEmailRequest request);

    record SendEmailRequest(String from, List<String> to, String subject, String text) { }
}
