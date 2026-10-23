package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.commandservices.OrganizationCommandService;
import com.takodu.api.organization.application.queryservices.OrganizationQueryService;
import com.takodu.api.organization.domain.model.queries.GetJoinedOrganizationsQuery;
import com.takodu.api.organization.interfaces.rest.resources.CreateOrganizationResource;
import com.takodu.api.organization.interfaces.rest.resources.JoinedOrganizationResource;
import com.takodu.api.organization.interfaces.rest.resources.OrganizationResource;
import com.takodu.api.organization.interfaces.rest.transform.CreateOrganizationCommandFromResourceAssembler;
import com.takodu.api.organization.interfaces.rest.transform.OrganizationResourceFromEntityAssembler;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.UUID;

/** The caller's organizations. These endpoints need no {@code X-Organization-Id}: they come before choosing one. */
@Path("/api/v1/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Organizations", description = "Businesses the caller founded or joined")
public class OrganizationsController {
    private final OrganizationCommandService organizationCommandService;
    private final OrganizationQueryService organizationQueryService;
    private final JsonWebToken accessToken;

    public OrganizationsController(OrganizationCommandService organizationCommandService,
                                   OrganizationQueryService organizationQueryService, JsonWebToken accessToken) {
        this.organizationCommandService = organizationCommandService;
        this.organizationQueryService = organizationQueryService;
        this.accessToken = accessToken;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Create organization", description = "Creates the organization, its first establishment and the caller as owner (O1), on a trial plan.")
    @APIResponse(responseCode = "201", description = "Organization created, or the one an earlier request with the same key created",
            content = @Content(schema = @Schema(implementation = OrganizationResource.class)))
    @APIResponse(responseCode = "400", description = "Blank name, unknown time zone, or missing Idempotency-Key")
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    @APIResponse(responseCode = "409", description = "The same request is still running")
    public OrganizationResource create(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                       @Valid CreateOrganizationResource resource) {
        var command = CreateOrganizationCommandFromResourceAssembler.toCommandFromResource(callerId(), idempotencyKey, resource);
        return OrganizationResourceFromEntityAssembler.toResourceFromEntity(organizationCommandService.handle(command));
    }

    @GET
    @Operation(summary = "List my organizations", description = "Organizations where the caller is an active member, with their role.")
    @APIResponse(responseCode = "200", description = "The caller's organizations, by name",
            content = @Content(schema = @Schema(implementation = JoinedOrganizationResource.class)))
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    public List<JoinedOrganizationResource> getMyOrganizations() {
        return organizationQueryService.handle(new GetJoinedOrganizationsQuery(callerId())).stream()
                .map(OrganizationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    private UserId callerId() {
        return new UserId(UUID.fromString(accessToken.getSubject()));
    }
}
