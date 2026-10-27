package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.organization.application.commandservices.MemberCommandService;
import com.takodu.api.organization.application.queryservices.MemberQueryService;
import com.takodu.api.organization.domain.model.commands.RemoveMemberCommand;
import com.takodu.api.organization.domain.model.commands.RenameOwnMembershipCommand;
import com.takodu.api.organization.domain.model.commands.RenewInvitationCommand;
import com.takodu.api.organization.domain.model.queries.GetCurrentMembersQuery;
import com.takodu.api.organization.interfaces.rest.resources.InviteMemberResource;
import com.takodu.api.organization.interfaces.rest.resources.MemberResource;
import com.takodu.api.organization.interfaces.rest.resources.RenameOwnMembershipResource;
import com.takodu.api.organization.interfaces.rest.resources.UpdateMemberResource;
import com.takodu.api.organization.interfaces.rest.transform.InviteMemberCommandFromResourceAssembler;
import com.takodu.api.organization.interfaces.rest.transform.MemberResourceFromEntityAssembler;
import com.takodu.api.organization.interfaces.rest.transform.UpdateMemberCommandFromResourceAssembler;
import com.takodu.api.shared.application.tenancy.TenantContext;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
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
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/members")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Members", description = "The team of the caller's organization and its invitations")
public class MembersController {
    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;
    private final TenantContext tenantContext;

    public MembersController(MemberCommandService memberCommandService, MemberQueryService memberQueryService,
                             TenantContext tenantContext) {
        this.memberCommandService = memberCommandService;
        this.memberQueryService = memberQueryService;
        this.tenantContext = tenantContext;
    }

    @GET
    @Operation(summary = "List members", description = "Active members and pending invitations.")
    @APIResponse(responseCode = "200", description = "Members by display name",
            content = @Content(schema = @Schema(implementation = MemberResource.class)))
    public List<MemberResource> getMembers() {
        return memberQueryService.handle(new GetCurrentMembersQuery(tenantContext.organizationId())).stream()
                .map(MemberResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Operation(summary = "Invite member", description = "Owner or manager. Emails a link valid 7 days (O10).")
    @APIResponse(responseCode = "201", description = "Invitation sent, or the one an earlier request with the same key created",
            content = @Content(schema = @Schema(implementation = MemberResource.class)))
    @APIResponse(responseCode = "400", description = "Invalid email or role, staff without establishments, or missing Idempotency-Key")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "A staff establishment is not active in the organization")
    @APIResponse(responseCode = "409", description = "MEMBER_CONFLICT: the email is already a member or invited (O11)")
    @APIResponse(responseCode = "422", description = "OWNER_CHANGE_NOT_ALLOWED: owners are not invited (O12)")
    public MemberResource invite(@HeaderParam("Idempotency-Key") UUID idempotencyKey, @Valid InviteMemberResource resource) {
        var command = InviteMemberCommandFromResourceAssembler.toCommandFromResource(tenantContext.organizationId(),
                tenantContext.memberId(), idempotencyKey, resource);
        return MemberResourceFromEntityAssembler.toResourceFromEntity(memberCommandService.handle(command));
    }

    @POST
    @Path("/{memberId}/invitation-renewals")
    @Operation(summary = "Resend invitation", description = "Owner or manager. Emails a new link valid 7 days; the old link stops working.")
    @APIResponse(responseCode = "200", description = "New link sent",
            content = @Content(schema = @Schema(implementation = MemberResource.class)))
    @APIResponse(responseCode = "400", description = "Missing Idempotency-Key")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Member not found in the organization")
    @APIResponse(responseCode = "422", description = "INVITATION_NOT_PENDING: the invitation was accepted")
    public MemberResource renewInvitation(@HeaderParam("Idempotency-Key") UUID idempotencyKey,
                                          @PathParam("memberId") UUID memberId) {
        var command = new RenewInvitationCommand(tenantContext.organizationId(), tenantContext.memberId(),
                new MemberId(memberId), new IdempotencyKey(idempotencyKey));
        return MemberResourceFromEntityAssembler.toResourceFromEntity(memberCommandService.handle(command));
    }

    @PUT
    @Path("/me")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Rename yourself", description = "Any member. Changes the name the team sees for the caller in this organization only; other organizations keep theirs.")
    @APIResponse(responseCode = "200", description = "Member renamed",
            content = @Content(schema = @Schema(implementation = MemberResource.class)))
    @APIResponse(responseCode = "400", description = "Blank or too long name")
    public MemberResource renameSelf(@Valid RenameOwnMembershipResource resource) {
        var command = new RenameOwnMembershipCommand(tenantContext.organizationId(), tenantContext.memberId(),
                resource.displayName());
        return MemberResourceFromEntityAssembler.toResourceFromEntity(memberCommandService.handle(command));
    }

    @PUT
    @Path("/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update member", description = "Owner or manager. Name, bookability, role and staff establishments; the owner's role cannot change (O12).")
    @APIResponse(responseCode = "200", description = "Member updated",
            content = @Content(schema = @Schema(implementation = MemberResource.class)))
    @APIResponse(responseCode = "400", description = "Blank name, unknown role, or staff without establishments")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Member, or a staff establishment, not found in the organization")
    @APIResponse(responseCode = "422", description = "OWNER_CHANGE_NOT_ALLOWED: transfer ownership instead")
    public MemberResource update(@PathParam("memberId") UUID memberId, @Valid UpdateMemberResource resource) {
        var command = UpdateMemberCommandFromResourceAssembler.toCommandFromResource(tenantContext.organizationId(),
                memberId, resource);
        return MemberResourceFromEntityAssembler.toResourceFromEntity(memberCommandService.handle(command));
    }

    @DELETE
    @Path("/{memberId}")
    @ResponseStatus(204)
    @Operation(summary = "Remove member", description = "Owner or manager. Removes a member or revokes a pending invitation; history is kept.")
    @APIResponse(responseCode = "204", description = "Member removed")
    @APIResponse(responseCode = "403", description = "The caller is staff")
    @APIResponse(responseCode = "404", description = "Member not found in the organization")
    @APIResponse(responseCode = "422", description = "OWNER_CHANGE_NOT_ALLOWED, or MEMBER_HAS_UPCOMING_APPOINTMENTS")
    public void remove(@PathParam("memberId") UUID memberId) {
        memberCommandService.handle(new RemoveMemberCommand(tenantContext.organizationId(), new MemberId(memberId)));
    }
}
