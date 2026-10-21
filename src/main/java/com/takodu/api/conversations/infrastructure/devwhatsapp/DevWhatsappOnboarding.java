package com.takodu.api.conversations.infrastructure.devwhatsapp;

import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.OnboardedNumber;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappOnboarding;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.shared.infrastructure.crypto.TokenCipher;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dev mode without Meta ({@code takodu.meta.enabled=false}): accepts any signup, so the inbox can be tried with
 * webhooks replayed by hand. Set {@code TAKODU_META_ENABLED=true} with real credentials to use Meta instead.
 */
@ApplicationScoped
@IfBuildProfile("dev")
@IfBuildProperty(name = "takodu.meta.enabled", stringValue = "false")
public class DevWhatsappOnboarding implements WhatsappOnboarding {
    private final TokenCipher tokenCipher;

    public DevWhatsappOnboarding(TokenCipher tokenCipher) {
        this.tokenCipher = tokenCipher;
    }

    @Override
    public OnboardedNumber onboard(String code, String wabaId, String phoneNumberId, LineMode mode, String pin) {
        Log.infof("Dev signup accepted for phone number id %s", phoneNumberId);
        return new OnboardedNumber("+51 900 000 000", new EncryptedToken(tokenCipher.encrypt("dev-token")));
    }

    @Override
    public void unsubscribe(WhatsappLine line) {
        Log.infof("Dev unsubscribe for line %s", line.getId().value());
    }
}
