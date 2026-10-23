package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.application.commandservices.WhatsappWebhookCommandService;
import com.takodu.api.conversations.domain.model.commands.ReceiveWhatsappWebhookCommand;
import com.takodu.api.conversations.domain.model.commands.VerifyWhatsappWebhookCommand;
import com.takodu.api.shared.interfaces.rest.idempotency.WithoutIdempotencyKey;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

/** Meta's single webhook for every connected line. It takes no access token; the signature authenticates Meta. */
@Path("/webhooks/whatsapp")
@Tag(name = "WhatsApp webhook", description = "Called by Meta, not by the app")
public class WhatsappWebhookController {
    private final WhatsappWebhookCommandService whatsappWebhookCommandService;

    public WhatsappWebhookController(WhatsappWebhookCommandService whatsappWebhookCommandService) {
        this.whatsappWebhookCommandService = whatsappWebhookCommandService;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Verify the webhook", description = "Meta's handshake: echoes hub.challenge when hub.verify_token matches.")
    @APIResponse(responseCode = "200", description = "The challenge")
    @APIResponse(responseCode = "401", description = "WEBHOOK_VERIFICATION_FAILED")
    public String verify(@QueryParam("hub.mode") String mode, @QueryParam("hub.verify_token") String verifyToken,
                         @QueryParam("hub.challenge") String challenge) {
        return whatsappWebhookCommandService.handle(new VerifyWhatsappWebhookCommand(mode, verifyToken, challenge));
    }

    @POST
    @Consumes(MediaType.WILDCARD)
    @ResponseStatus(200)
    @WithoutIdempotencyKey("Meta redelivers webhooks; every wamid is stored once (W4) and statuses only move forward (W10)")
    @Operation(summary = "Receive events",
            description = "Checks X-Hub-Signature-256 on the raw body, stores everything in one transaction and answers. Calls nothing outside the database.")
    @APIResponse(responseCode = "200", description = "Stored, or ignored for an unknown or disconnected line")
    @APIResponse(responseCode = "400", description = "Signed body that is not JSON")
    @APIResponse(responseCode = "401", description = "WEBHOOK_SIGNATURE_INVALID: nothing stored")
    public void receive(@HeaderParam("X-Hub-Signature-256") String signature, byte[] body) {
        whatsappWebhookCommandService.handle(new ReceiveWhatsappWebhookCommand(body, signature));
    }
}
