package com.takodu.api.conversations.infrastructure.devwhatsapp;

import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappGateway;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/** Dev mode without Meta: every message is accepted. Logs the wamid only, never the text or the number. */
@ApplicationScoped
@IfBuildProfile("dev")
@IfBuildProperty(name = "takodu.meta.enabled", stringValue = "false")
public class DevWhatsappGateway implements WhatsappGateway {

    @Override
    public Wamid sendText(WhatsappLine line, String to, String body) {
        var wamid = new Wamid("wamid.DEV" + UUID.randomUUID().toString().replace("-", ""));
        Log.infof("Dev send on line %s accepted as %s", line.getId().value(), wamid.value());
        return wamid;
    }
}
