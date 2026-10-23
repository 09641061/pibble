package com.takodu.api.architecture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The prod profile reads every secret from the environment without a default, so Quarkus refuses to start when one is
 * missing instead of running with a dev value. Starting the prod profile is too slow for a test; this checks the file.
 */
class ProductionConfigTest {
    /** Property and the variable production must set. */
    static final Map<String, String> REQUIRED = Map.ofEntries(
            Map.entry("quarkus.datasource.username", "DATABASE_USER"),
            Map.entry("quarkus.datasource.password", "DATABASE_PASSWORD"),
            Map.entry("takodu.app-url", "APP_URL"),
            Map.entry("takodu.email.from", "EMAIL_FROM"),
            Map.entry("takodu.email.resend-api-key", "RESEND_API_KEY"),
            Map.entry("mp.jwt.verify.publickey.location", "JWT_PUBLIC_KEY_LOCATION"),
            Map.entry("smallrye.jwt.sign.key.location", "JWT_PRIVATE_KEY_LOCATION"),
            Map.entry("takodu.meta.app-id", "META_APP_ID"),
            Map.entry("takodu.meta.app-secret", "META_APP_SECRET"),
            Map.entry("takodu.meta.webhook-verify-token", "META_WEBHOOK_VERIFY_TOKEN"),
            Map.entry("takodu.token-key", "TOKEN_ENCRYPTION_KEY"),
            Map.entry("takodu.media.dir", "MEDIA_DIR"),
            Map.entry("quarkus.langchain4j.openai.api-key", "LLM_API_KEY"));

    static Properties config;

    @BeforeAll
    static void load() throws IOException {
        config = new Properties();
        try (var in = ProductionConfigTest.class.getResourceAsStream("/application.properties")) {
            config.load(in);
        }
    }

    @Test
    void everySecretComesFromTheEnvironmentWithoutDefault() {
        REQUIRED.forEach((property, variable) ->
                assertEquals("${" + variable + "}", config.getProperty("%prod." + property), property));
    }

    @Test
    void productionHidesTheApiDescriptionAndLogsJson() {
        assertEquals("false", config.getProperty("%prod.quarkus.swagger-ui.enabled"));
        assertEquals("false", config.getProperty("%prod.quarkus.smallrye-openapi.enabled"));
        assertEquals("true", config.getProperty("%prod.quarkus.log.console.json.enabled"));
    }

    @Test
    void browsersMayCallOnlyFromTheWebApp() {
        assertEquals("true", config.getProperty("quarkus.http.cors.enabled"));
        assertEquals("${takodu.app-url}", config.getProperty("quarkus.http.cors.origins"));
    }
}
