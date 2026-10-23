package com.takodu.api.assistant.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

public class OrganizationNotFoundException extends ResourceNotFoundException {
    public OrganizationNotFoundException(OrganizationId organizationId) {
        super("Organization", organizationId.value());
    }
}
