package com.takodu.api.conversations.infrastructure.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.WhatsappWebhookReader;
import com.takodu.api.conversations.domain.model.commands.WhatsappWebhookCommand;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/** Active in every profile: tests sign their recorded payloads with the test app secret. */
@ApplicationScoped
public class MetaWebhookReader implements WhatsappWebhookReader {
    private final MetaSignatureVerifier signatureVerifier;
    private final MetaWebhookParser parser;
    private final byte[] verifyToken;

    public MetaWebhookReader(ObjectMapper objectMapper,
                             @ConfigProperty(name = "takodu.meta.app-secret") String appSecret,
                             @ConfigProperty(name = "takodu.meta.webhook-verify-token") String verifyToken) {
        this.signatureVerifier = new MetaSignatureVerifier(appSecret);
        this.parser = new MetaWebhookParser(objectMapper);
        this.verifyToken = verifyToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isAuthentic(byte[] body, String signature) {
        return signatureVerifier.verify(body, signature);
    }

    @Override
    public boolean acceptsVerifyToken(String token) {
        return token != null && MessageDigest.isEqual(verifyToken, token.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<WhatsappWebhookCommand> read(byte[] body) {
        return parser.parse(body);
    }
}
