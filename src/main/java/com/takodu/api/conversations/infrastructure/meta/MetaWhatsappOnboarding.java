package com.takodu.api.conversations.infrastructure.meta;

import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.OnboardedNumber;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappOnboarding;
import com.takodu.api.conversations.domain.exceptions.WhatsappSignupFailedException;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.shared.infrastructure.crypto.TokenCipher;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.function.Supplier;

/** Embedded Signup through the Graph API. The plain token lives only inside this call and is encrypted at once. */
@ApplicationScoped
@IfBuildProperty(name = "takodu.meta.enabled", stringValue = "true", enableIfMissing = true)
public class MetaWhatsappOnboarding implements WhatsappOnboarding {
    private static final String WHATSAPP = "whatsapp";

    private final MetaGraphClient metaGraphClient;
    private final TokenCipher tokenCipher;
    private final String version;
    private final String appId;
    private final String appSecret;

    public MetaWhatsappOnboarding(@RestClient MetaGraphClient metaGraphClient, TokenCipher tokenCipher,
                                  @ConfigProperty(name = "takodu.meta.graph-version") String version,
                                  @ConfigProperty(name = "takodu.meta.app-id") String appId,
                                  @ConfigProperty(name = "takodu.meta.app-secret") String appSecret) {
        this.metaGraphClient = metaGraphClient;
        this.tokenCipher = tokenCipher;
        this.version = version;
        this.appId = appId;
        this.appSecret = appSecret;
    }

    @Override
    public OnboardedNumber onboard(String code, String wabaId, String phoneNumberId, LineMode mode, String pin) {
        var token = step("exchanging the signup code",
                () -> metaGraphClient.exchangeCode(version, appId, appSecret, code)).accessToken();
        if (token == null || token.isBlank()) throw new WhatsappSignupFailedException("Meta returned no business token");
        var bearer = "Bearer " + token;
        step("subscribing to the account's webhooks", () -> {
            metaGraphClient.subscribeApp(version, wabaId, bearer);
            return null;
        });
        var number = step("reading the number", () -> metaGraphClient.fetchPhoneNumber(version, phoneNumberId,
                "display_phone_number", bearer));
        if (mode == LineMode.COEXISTENCE) {
            step("requesting the chat history", () -> {
                metaGraphClient.requestAppData(version, phoneNumberId, bearer,
                        new MetaGraphClient.AppDataRequest(WHATSAPP, "history"));
                return null;
            });
        } else {
            step("registering the number", () -> {
                metaGraphClient.register(version, phoneNumberId, bearer, new MetaGraphClient.RegisterRequest(WHATSAPP, pin));
                return null;
            });
        }
        return new OnboardedNumber(number.displayPhoneNumber(), new EncryptedToken(tokenCipher.encrypt(token)));
    }

    @Override
    public void unsubscribe(WhatsappLine line) {
        metaGraphClient.unsubscribeApp(version, line.getWabaId(),
                "Bearer " + tokenCipher.decrypt(line.getAccessToken().value()));
    }

    /** Turns Meta's refusal into a message for the owner; the response body is not copied, so no token leaks. */
    private static <T> T step(String what, Supplier<T> call) {
        try {
            return call.get();
        } catch (WebApplicationException e) {
            throw new WhatsappSignupFailedException("Meta refused %s (HTTP %d); start the signup again"
                    .formatted(what, e.getResponse().getStatus()));
        } catch (ProcessingException e) {
            throw new WhatsappSignupFailedException("Meta did not answer while %s; start the signup again".formatted(what));
        }
    }
}
