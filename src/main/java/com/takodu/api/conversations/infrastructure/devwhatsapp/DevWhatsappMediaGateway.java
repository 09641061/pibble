package com.takodu.api.conversations.infrastructure.devwhatsapp;

import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappMediaGateway;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/** Dev mode without Meta: every media is an empty file, so the inbox shows it as downloaded. */
@ApplicationScoped
@IfBuildProfile("dev")
@IfBuildProperty(name = "takodu.meta.enabled", stringValue = "false")
public class DevWhatsappMediaGateway implements WhatsappMediaGateway {

    @Override
    public InputStream download(WhatsappLine line, String mediaId, long maxBytes) {
        Log.infof("Dev media download on line %s accepted", line.getId().value());
        return new ByteArrayInputStream(new byte[0]);
    }
}
