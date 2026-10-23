package com.takodu.api.assistant.interfaces.rest;

import com.takodu.api.assistant.application.queryservices.AssistantUsageQueryService;
import com.takodu.api.assistant.domain.model.queries.GetEstablishmentUsageQuery;
import com.takodu.api.assistant.domain.model.valueobjects.UsagePeriod;
import com.takodu.api.assistant.interfaces.rest.resources.AssistantUsageResource;
import com.takodu.api.assistant.interfaces.rest.transform.AssistantUsageResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.interfaces.rest.transform.QueryParameters;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/v1/establishments/{establishmentId}/assistant/usage")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Assistant", description = "The AI assistant that answers an establishment's customers on WhatsApp")
public class EstablishmentAssistantUsageController {
    private final AssistantUsageQueryService assistantUsageQueryService;
    private final TenantContext tenantContext;

    public EstablishmentAssistantUsageController(AssistantUsageQueryService assistantUsageQueryService,
                                                 TenantContext tenantContext) {
        this.assistantUsageQueryService = assistantUsageQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "Get assistant usage", description = "Owner or manager. Answered turns, tokens and tool calls per day in the establishment's time zone (A9), also while the plan is inactive.")
    @APIResponse(responseCode = "200", description = "Usage for the days from and to, both included",
            content = @Content(schema = @Schema(implementation = AssistantUsageResource.class)))
    @APIResponse(responseCode = "400", description = "from or to missing or not a date, to before from, or more than 366 days")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Establishment not found in the organization")
    public AssistantUsageResource getUsage(@PathParam("establishmentId") UUID establishmentId,
                                           @QueryParam("from") String from, @QueryParam("to") String to) {
        var period = new UsagePeriod(QueryParameters.date("from", from), QueryParameters.date("to", to));
        var usage = assistantUsageQueryService.handle(new GetEstablishmentUsageQuery(tenantContext.organizationId(),
                new EstablishmentId(establishmentId), period));
        return AssistantUsageResourceFromEntityAssembler.toResourceFromEntity(usage);
    }
}
