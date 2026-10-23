package com.takodu.api.assistant.interfaces.rest;

import com.takodu.api.assistant.application.commandservices.AssistantSettingsCommandService;
import com.takodu.api.assistant.application.queryservices.AssistantSettingsQueryService;
import com.takodu.api.assistant.domain.model.queries.GetAssistantSettingsQuery;
import com.takodu.api.assistant.interfaces.rest.resources.AssistantSettingsDetailsResource;
import com.takodu.api.assistant.interfaces.rest.resources.AssistantSettingsResource;
import com.takodu.api.assistant.interfaces.rest.transform.AssistantSettingsResourceFromEntityAssembler;
import com.takodu.api.assistant.interfaces.rest.transform.ConfigureAssistantCommandFromResourceAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/v1/establishments/{establishmentId}/assistant")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Assistant", description = "The AI assistant that answers an establishment's customers on WhatsApp")
public class EstablishmentAssistantController {
    private final AssistantSettingsCommandService assistantSettingsCommandService;
    private final AssistantSettingsQueryService assistantSettingsQueryService;
    private final TenantContext tenantContext;

    public EstablishmentAssistantController(AssistantSettingsCommandService assistantSettingsCommandService,
                                            AssistantSettingsQueryService assistantSettingsQueryService,
                                            TenantContext tenantContext) {
        this.assistantSettingsCommandService = assistantSettingsCommandService;
        this.assistantSettingsQueryService = assistantSettingsQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "Get assistant settings", description = "Owner or manager. Off with no business info until configured.")
    @APIResponse(responseCode = "200", description = "Whether the assistant answers, and its business info",
            content = @Content(schema = @Schema(implementation = AssistantSettingsResource.class)))
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    public AssistantSettingsResource getSettings(@PathParam("establishmentId") UUID establishmentId) {
        var settings = assistantSettingsQueryService.handle(new GetAssistantSettingsQuery(
                tenantContext.organizationId(), new EstablishmentId(establishmentId)));
        return AssistantSettingsResourceFromEntityAssembler.toResourceFromEntity(settings);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Configure the assistant", description = "Owner or manager. Turns it on or off and replaces the business info (A1).")
    @APIResponse(responseCode = "200", description = "Settings saved",
            content = @Content(schema = @Schema(implementation = AssistantSettingsResource.class)))
    @APIResponse(responseCode = "400", description = "enabled missing, or business info over 4,000 characters")
    @APIResponse(responseCode = "402", description = "The plan is not active (O6)")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    @APIResponse(responseCode = "409", description = "Someone else saved the first settings meanwhile")
    @APIResponse(responseCode = "422", description = "ESTABLISHMENT_ARCHIVED")
    public AssistantSettingsResource configure(@PathParam("establishmentId") UUID establishmentId,
                                               @Valid AssistantSettingsDetailsResource resource) {
        var command = ConfigureAssistantCommandFromResourceAssembler.toCommandFromResource(
                tenantContext.organizationId(), establishmentId, resource);
        return AssistantSettingsResourceFromEntityAssembler.toResourceFromEntity(
                assistantSettingsCommandService.handle(command));
    }
}
