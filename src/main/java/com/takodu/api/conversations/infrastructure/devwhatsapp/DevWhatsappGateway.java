package com.takodu.api.conversations.infrastructure.devwhatsapp;

import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappGateway;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/** Dev mode without Meta: every message is accepted. Logs the wamid only, never the text or the number. */
@ApplicationScoped
@IfBuildProfile("dev")
@IfBuildProperty(name = "takodu.meta.enabled", stringValue = "false")
public class DevWhatsappGateway implements WhatsappGateway {

    @Override
    public Wamid sendText(WhatsappLine line, String to, String body) {
        return accept(line);
    }

    @Override
    public Wamid sendButtons(WhatsappLine line, String to, String body, List<String> titles) {
        return accept(line);
    }

    @Override
    public Wamid sendList(WhatsappLine line, String to, String body, String buttonLabel, List<String> rows) {
        return accept(line);
    }

    @Override
    public Wamid sendTemplate(WhatsappLine line, String to, String templateName, String language,
                              List<String> parameters) {
        return accept(line);
    }

    private static Wamid accept(WhatsappLine line) {
        var wamid = new Wamid("wamid.DEV" + UUID.randomUUID().toString().replace("-", ""));
        Log.infof("Dev send on line %s accepted as %s", line.getId().value(), wamid.value());
        return wamid;
    }
}
