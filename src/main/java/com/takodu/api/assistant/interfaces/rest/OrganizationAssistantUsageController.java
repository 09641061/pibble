package com.takodu.api.assistant.interfaces.rest;

import com.takodu.api.assistant.application.queryservices.AssistantUsageQueryService;
import com.takodu.api.assistant.domain.model.queries.GetOrganizationUsageQuery;
import com.takodu.api.assistant.domain.model.valueobjects.UsagePeriod;
import com.takodu.api.assistant.interfaces.rest.resources.OrganizationAssistantUsageResource;
import com.takodu.api.assistant.interfaces.rest.transform.AssistantUsageResourceFromEntityAssembler;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
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

/** A9 for the Takodu team, to compare model cost with what a business pays. {@code OperatorFilter} admits only operators. */
@Path("/internal/v1/organizations/{organizationId}/assistant-usage")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Internal", description = "Takodu team only: plans and usage across organizations")
public class OrganizationAssistantUsageController {
    private final AssistantUsageQueryService assistantUsageQueryService;

    public OrganizationAssistantUsageController(AssistantUsageQueryService assistantUsageQueryService) {
        this.assistantUsageQueryService = assistantUsageQueryService;
    }

    @GET
    @Operation(summary = "Get an organization's assistant usage", description = "Each establishment's answered turns, tokens and tool calls, in its own time zone, and their sum.")
    @APIResponse(responseCode = "200", description = "Usage for the days from and to, both included",
            content = @Content(schema = @Schema(implementation = OrganizationAssistantUsageResource.class)))
    @APIResponse(responseCode = "400", description = "from or to missing or not a date, to before from, or more than 366 days")
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    @APIResponse(responseCode = "403", description = "The caller is not a platform operator")
    @APIResponse(responseCode = "404", description = "No organization has this id")
    public OrganizationAssistantUsageResource getUsage(@PathParam("organizationId") UUID organizationId,
                                                       @QueryParam("from") String from, @QueryParam("to") String to) {
        var id = new OrganizationId(organizationId);
        var period = new UsagePeriod(QueryParameters.date("from", from), QueryParameters.date("to", to));
        return AssistantUsageResourceFromEntityAssembler.toResourceFromEntity(id, period,
                assistantUsageQueryService.handle(new GetOrganizationUsageQuery(id, period)));
    }
}
