package com.takodu.api.conversations.interfaces.rest.transform;

import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.interfaces.rest.resources.WhatsappLineResource;

import java.util.Locale;

public final class WhatsappLineResourceFromEntityAssembler {
    private WhatsappLineResourceFromEntityAssembler() { }

    public static WhatsappLineResource toResourceFromEntity(WhatsappLine line) {
        return new WhatsappLineResource(line.getId().value(), line.getEstablishmentId().value(), line.getDisplayPhone(),
                line.getMode().name().toLowerCase(Locale.ROOT), line.isConnected() ? "connected" : "disconnected",
                line.getConnectedAt());
    }
}
