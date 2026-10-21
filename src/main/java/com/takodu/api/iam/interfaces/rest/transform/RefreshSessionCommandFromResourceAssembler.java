package com.takodu.api.iam.interfaces.rest.transform;

import com.takodu.api.iam.domain.model.commands.RefreshSessionCommand;
import com.takodu.api.iam.interfaces.rest.resources.RefreshSessionResource;

public final class RefreshSessionCommandFromResourceAssembler {
    private RefreshSessionCommandFromResourceAssembler() { }

    public static RefreshSessionCommand toCommandFromResource(RefreshSessionResource resource) {
        return new RefreshSessionCommand(resource.refreshToken());
    }
}
