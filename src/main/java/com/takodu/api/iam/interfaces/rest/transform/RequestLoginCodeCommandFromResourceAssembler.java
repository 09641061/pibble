package com.takodu.api.iam.interfaces.rest.transform;

import com.takodu.api.iam.domain.model.commands.RequestLoginCodeCommand;
import com.takodu.api.iam.domain.model.valueobjects.Email;
import com.takodu.api.iam.interfaces.rest.resources.RequestLoginCodeResource;

public final class RequestLoginCodeCommandFromResourceAssembler {
    private RequestLoginCodeCommandFromResourceAssembler() { }

    public static RequestLoginCodeCommand toCommandFromResource(RequestLoginCodeResource resource) {
        return new RequestLoginCodeCommand(new Email(resource.email()));
    }
}
