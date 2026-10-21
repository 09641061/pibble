package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.commandservices.MemberCommandService;
import com.takodu.api.organization.domain.model.commands.AcceptInvitationCommand;
import com.takodu.api.organization.interfaces.rest.resources.AcceptInvitationResource;
import com.takodu.api.organization.interfaces.rest.resources.JoinedOrganizationResource;
import com.takodu.api.organization.interfaces.rest.transform.OrganizationResourceFromEntityAssembler;
import com.takodu.api.shared.domain.model.valueobjects.UserId;
import com.takodu.api.shared.interfaces.rest.idempotency.WithoutIdempotencyKey;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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

import java.util.UUID;

/** Needs no {@code X-Organization-Id}: the caller is not a member until the invitation is accepted. */
@Path("/api/v1/invitation-acceptances")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Members", description = "The team of the caller's organization and its invitations")
public class InvitationAcceptancesController {
    private final MemberCommandService memberCommandService;
    private final JsonWebToken accessToken;

    public InvitationAcceptancesController(MemberCommandService memberCommandService, JsonWebToken accessToken) {
        this.memberCommandService = memberCommandService;
        this.accessToken = accessToken;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @WithoutIdempotencyKey("accepting an accepted invitation again as the same user returns the same membership")
    @Operation(summary = "Accept invitation", description = "Joins the organization of the invitation; the caller must have signed in with the invited email (O10).")
    @APIResponse(responseCode = "201", description = "Joined",
            content = @Content(schema = @Schema(implementation = JoinedOrganizationResource.class)))
    @APIResponse(responseCode = "401", description = "Missing or expired access token")
    @APIResponse(responseCode = "403", description = "The caller signed in with another email")
    @APIResponse(responseCode = "404", description = "Unknown or revoked invitation")
    @APIResponse(responseCode = "409", description = "Another user accepted the invitation")
    @APIResponse(responseCode = "422", description = "INVITATION_EXPIRED")
    public JoinedOrganizationResource accept(@Valid AcceptInvitationResource resource) {
        var command = new AcceptInvitationCommand(new UserId(UUID.fromString(accessToken.getSubject())), resource.token().strip());
        return OrganizationResourceFromEntityAssembler.toResourceFromEntity(memberCommandService.handle(command));
    }
}
