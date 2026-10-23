package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.commandservices.MemberCommandService;
import com.takodu.api.organization.domain.model.commands.TransferOwnershipCommand;
import com.takodu.api.organization.interfaces.rest.resources.MemberResource;
import com.takodu.api.organization.interfaces.rest.resources.TransferOwnershipResource;
import com.takodu.api.organization.interfaces.rest.transform.MemberResourceFromEntityAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.UUID;

@Path("/api/v1/ownership-transfers")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Members", description = "The team of the caller's organization and its invitations")
public class OwnershipTransfersController {
    private final MemberCommandService memberCommandService;
    private final TenantContext tenantContext;

    public OwnershipTransfersController(MemberCommandService memberCommandService, TenantContext tenantContext) {
        this.memberCommandService = memberCommandService;
        this.tenantContext = tenantContext;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Transfer ownership", description = "Owner only. The member becomes owner and the caller a manager, in one step (O12).")
    @APIResponse(responseCode = "201", description = "Ownership transferred; the body is the new owner",
            content = @Content(schema = @Schema(implementation = MemberResource.class)))
    @APIResponse(responseCode = "400", description = "Missing Idempotency-Key")
    @APIResponse(responseCode = "403", description = "The caller is not the owner")
    @APIResponse(responseCode = "404", description = "Member not found in the organization")
    @APIResponse(responseCode = "409", description = "A concurrent transfer finished first")
    @APIResponse(responseCode = "422", description = "MEMBER_NOT_ACTIVE: the invitation was not accepted yet")
    public MemberResource transfer(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                   @Valid TransferOwnershipResource resource) {
        var command = new TransferOwnershipCommand(tenantContext.organizationId(), tenantContext.memberId(),
                new MemberId(resource.newOwnerId()), new IdempotencyKey(idempotencyKey));
        return MemberResourceFromEntityAssembler.toResourceFromEntity(memberCommandService.handle(command));
    }
}
