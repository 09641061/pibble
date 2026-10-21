package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.commands.UpdateMemberCommand;
import com.takodu.api.organization.interfaces.rest.resources.UpdateMemberResource;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.UUID;

public final class UpdateMemberCommandFromResourceAssembler {
    private UpdateMemberCommandFromResourceAssembler() { }

    public static UpdateMemberCommand toCommandFromResource(OrganizationId organizationId, UUID memberId,
                                                            UpdateMemberResource resource) {
        return new UpdateMemberCommand(organizationId, new MemberId(memberId), resource.displayName(),
                resource.bookable(), InviteMemberCommandFromResourceAssembler.role(resource.role()),
                InviteMemberCommandFromResourceAssembler.establishmentIds(resource.establishmentIds()));
    }
}
