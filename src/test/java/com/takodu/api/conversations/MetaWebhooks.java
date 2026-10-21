package com.takodu.api.conversations;

import com.takodu.api.conversations.infrastructure.meta.MetaSignatureVerifier;
import io.restassured.response.ValidatableResponse;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;

/**
 * Replays the anonymized Meta payloads in {@code src/test/resources/meta/}, signed with the test app secret.
 * Placeholders such as {@code ${PHONE_NUMBER_ID}} are filled per test.
 */
public final class MetaWebhooks {
    private MetaWebhooks() { }

    /** @return a new wamid, unique per call */
    public static String newWamid() {
        return "wamid.TEST" + UUID.randomUUID().toString().replace("-", "");
    }

    /** @return a random Peruvian mobile as Meta writes it, without {@code +} */
    public static String newWaId() {
        return "519" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    /**
     * @param values the placeholders without {@code ${}}; {@code WABA_ID} and {@code TIMESTAMP} (now) have defaults
     */
    public static String payload(String name, Map<String, String> values) {
        var all = new HashMap<String, String>();
        all.put("WABA_ID", TestWhatsapp.WABA_ID);
        all.put("TIMESTAMP", String.valueOf(Instant.now().getEpochSecond()));
        all.putAll(values);
        var json = read(name);
        for (var value : all.entrySet()) json = json.replace("${" + value.getKey() + "}", value.getValue());
        return json;
    }

    public static ValidatableResponse post(String body) {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        return post(bytes, new MetaSignatureVerifier(appSecret()).header(bytes));
    }

    public static ValidatableResponse post(byte[] body, String signature) {
        var request = given().contentType("application/json").body(body);
        if (signature != null) request = request.header("X-Hub-Signature-256", signature);
        return request.post("/webhooks/whatsapp").then();
    }

    private static String appSecret() {
        return ConfigProvider.getConfig().getValue("takodu.meta.app-secret", String.class);
    }

    private static String read(String name) {
        try (var in = MetaWebhooks.class.getResourceAsStream("/meta/" + name + ".json")) {
            if (in == null) throw new IllegalArgumentException("No recorded payload " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
