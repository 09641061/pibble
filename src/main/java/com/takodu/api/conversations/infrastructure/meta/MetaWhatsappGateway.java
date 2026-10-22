package com.takodu.api.conversations.infrastructure.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappGateway;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappSendException;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.Wamid;
import com.takodu.api.shared.infrastructure.crypto.TokenCipher;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sends through {@code POST /{phone-number-id}/messages}; the token is decrypted here and nowhere else. */
@ApplicationScoped
@IfBuildProperty(name = "takodu.meta.enabled", stringValue = "true", enableIfMissing = true)
public class MetaWhatsappGateway implements WhatsappGateway {
    /** Meta's codes for throttling and temporary outages; anything else in a 4xx will fail again. */
    private static final Set<Integer> RETRYABLE_CODES = Set.of(4, 80007, 130429, 131000, 131016, 131056, 133004);

    private final MetaGraphClient metaGraphClient;
    private final TokenCipher tokenCipher;
    private final ObjectMapper objectMapper;
    private final String version;

    public MetaWhatsappGateway(@RestClient MetaGraphClient metaGraphClient, TokenCipher tokenCipher,
                               ObjectMapper objectMapper,
                               @ConfigProperty(name = "takodu.meta.graph-version") String version) {
        this.metaGraphClient = metaGraphClient;
        this.tokenCipher = tokenCipher;
        this.objectMapper = objectMapper;
        this.version = version;
    }

    @Override
    public Wamid sendText(WhatsappLine line, String to, String body) {
        return send(line, Map.of("messaging_product", "whatsapp", "recipient_type", "individual", "to", to,
                "type", "text", "text", Map.of("preview_url", false, "body", body)));
    }

    /** Body parameters are positional text; the template's quick-reply buttons need none. */
    @Override
    public Wamid sendTemplate(WhatsappLine line, String to, String templateName, String language,
                              List<String> parameters) {
        var body = Map.of("type", "body", "parameters", parameters.stream()
                .map(text -> Map.of("type", "text", "text", text)).toList());
        return send(line, Map.of("messaging_product", "whatsapp", "recipient_type", "individual", "to", to,
                "type", "template", "template", Map.of("name", templateName, "language", Map.of("code", language),
                        "components", List.of(body))));
    }

    private Wamid send(WhatsappLine line, Map<String, Object> payload) {
        try {
            var response = metaGraphClient.sendMessage(version, line.getPhoneNumberId(),
                    "Bearer " + tokenCipher.decrypt(line.getAccessToken().value()), payload);
            var id = response.path("messages").path(0).path("id").asText();
            if (id.isEmpty()) throw new WhatsappSendException("Meta accepted the message without an id", false);
            return new Wamid(id);
        } catch (WebApplicationException e) {
            throw refusal(e);
        } catch (ProcessingException e) {
            throw new WhatsappSendException("Meta did not answer", true);
        }
    }

    /** Keeps Meta's error code and title, which name the problem (131047: window closed) without the message text. */
    private WhatsappSendException refusal(WebApplicationException e) {
        var status = e.getResponse().getStatus();
        JsonNode error;
        try {
            error = objectMapper.readTree(e.getResponse().readEntity(String.class)).path("error");
        } catch (Exception unreadable) {
            error = objectMapper.missingNode();
        }
        var code = error.path("code").asInt(-1);
        var title = error.path("error_data").path("details").asText(error.path("message").asText("HTTP " + status));
        var retryable = status >= 500 || status == 429 || RETRYABLE_CODES.contains(code);
        return new WhatsappSendException(code < 0 ? title : code + ": " + title, retryable);
    }
}
