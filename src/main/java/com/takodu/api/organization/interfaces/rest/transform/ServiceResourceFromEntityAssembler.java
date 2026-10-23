package com.takodu.api.organization.interfaces.rest.transform;

import com.takodu.api.organization.domain.model.aggregates.Service;
import com.takodu.api.organization.interfaces.rest.resources.ServiceResource;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;

import java.util.stream.Collectors;

public final class ServiceResourceFromEntityAssembler {
    private ServiceResourceFromEntityAssembler() { }

    public static ServiceResource toResourceFromEntity(Service service) {
        return new ServiceResource(service.getId().value(), service.getEstablishmentId().value(), service.getName(),
                (int) service.getDuration().toMinutes(), service.getPrice().cents(), service.getPrice().currency(),
                service.getInstructions(), service.isActive(),
                service.getPerformerIds().stream().map(MemberId::value).collect(Collectors.toSet()));
    }
}
