package com.takodu.api.conversations.application.internal.eventhandlers;

import com.takodu.api.conversations.application.commandservices.MediaCommandService;
import com.takodu.api.conversations.domain.model.commands.RemoveCustomerMediaCommand;
import com.takodu.api.customers.interfaces.events.CustomerDeletedIntegrationEvent;
import com.takodu.api.shared.domain.model.valueobjects.CustomerId;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class CustomerDeletedEventHandler {
    private final MediaCommandService mediaCommandService;

    public CustomerDeletedEventHandler(MediaCommandService mediaCommandService) {
        this.mediaCommandService = mediaCommandService;
    }

    /** C5: in customers' transaction, so the media is marked exactly when the deletion commits. */
    void on(@Observes CustomerDeletedIntegrationEvent event) {
        mediaCommandService.handle(new RemoveCustomerMediaCommand(new OrganizationId(event.organizationId()),
                new CustomerId(event.customerId())));
    }
}
