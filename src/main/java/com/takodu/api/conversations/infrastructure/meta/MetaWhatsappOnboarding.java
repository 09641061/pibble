package com.takodu.api.conversations.infrastructure.meta;

import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.OnboardedNumber;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappOnboarding;
import com.takodu.api.conversations.domain.exceptions.WhatsappSignupFailedException;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.conversations.domain.model.valueobjects.NotificationTemplate;
import com.takodu.api.conversations.domain.model.valueobjects.TemplateStatus;
import com.takodu.api.shared.infrastructure.crypto.TokenCipher;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
        return new OnboardedNumber(number.displayPhoneNumber(), new EncryptedToken(tokenCipher.encrypt(token)),
                ensureTemplates(wabaId, bearer));
    }

    @Override
    public void unsubscribe(WhatsappLine line) {
        metaGraphClient.unsubscribeApp(version, line.getWabaId(),
                "Bearer " + tokenCipher.decrypt(line.getAccessToken().value()));
    }

    /**
     * Creates each notification template the account lacks, and reads the status of the ones it has (a number
     * reconnected, or a second number of the same account). Best effort: a template Meta refuses is left out and its
     * notifications wait, so the line still connects for chatting.
     */
    private Map<NotificationTemplate, TemplateStatus> ensureTemplates(String wabaId, String bearer) {
        var statuses = new EnumMap<NotificationTemplate, TemplateStatus>(NotificationTemplate.class);
        for (var template : NotificationTemplate.values()) {
            try {
                var existing = metaGraphClient.fetchTemplates(version, wabaId, template.metaName(),
                                "name,language,status", bearer).data().stream()
                        .filter(found -> template.metaName().equals(found.name())
                                && NotificationTemplate.LANGUAGE.equals(found.language()))
                        .findFirst();
                var status = existing.orElseGet(() -> metaGraphClient.createTemplate(version, wabaId, bearer,
                        definition(template))).status();
                MetaTemplateStatuses.of(status).ifPresent(known -> statuses.put(template, known));
            } catch (WebApplicationException e) {
                Log.warnf("Meta refused template %s on account %s (HTTP %d)", template.metaName(), wabaId,
                        e.getResponse().getStatus());
            } catch (ProcessingException e) {
                Log.warnf("Meta did not answer creating template %s on account %s", template.metaName(), wabaId);
            }
        }
        return statuses;
    }

    /** A utility template in Spanish, with example values for Meta's review and the quick-reply buttons. */
    private static Map<String, Object> definition(NotificationTemplate template) {
        var components = new ArrayList<Map<String, Object>>();
        components.add(Map.of("type", "BODY", "text", template.body(),
                "example", Map.of("body_text", List.of(template.example()))));
        if (!template.buttons().isEmpty()) {
            components.add(Map.of("type", "BUTTONS", "buttons", template.buttons().stream()
                    .map(title -> Map.of("type", "QUICK_REPLY", "text", title)).toList()));
        }
        return Map.of("name", template.metaName(), "language", NotificationTemplate.LANGUAGE, "category", "UTILITY",
                "components", components);
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
