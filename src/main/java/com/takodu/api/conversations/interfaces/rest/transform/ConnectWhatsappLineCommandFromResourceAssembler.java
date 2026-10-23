package com.takodu.api.conversations.interfaces.rest.transform;

import com.takodu.api.conversations.domain.model.commands.ConnectWhatsappLineCommand;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.conversations.interfaces.rest.resources.ConnectWhatsappLineResource;
import com.takodu.api.shared.domain.model.valueobjects.EstablishmentId;
import com.takodu.api.shared.domain.model.valueobjects.IdempotencyKey;
import com.takodu.api.shared.domain.model.valueobjects.OrganizationId;

import java.util.Locale;
import java.util.UUID;

public final class ConnectWhatsappLineCommandFromResourceAssembler {
    private ConnectWhatsappLineCommandFromResourceAssembler() { }

    public static ConnectWhatsappLineCommand toCommandFromResource(OrganizationId organizationId, UUID establishmentId,
                                                                   UUID idempotencyKey,
                                                                   ConnectWhatsappLineResource resource) {
        var mode = resource.mode() == null ? null : LineMode.valueOf(resource.mode().toUpperCase(Locale.ROOT));
        return new ConnectWhatsappLineCommand(organizationId, new EstablishmentId(establishmentId), resource.code(),
                resource.wabaId(), resource.phoneNumberId(), mode, resource.pin(),
                idempotencyKey == null ? null : new IdempotencyKey(idempotencyKey));
    }
}
