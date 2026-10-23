package com.takodu.api.shared.interfaces.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/** Browsers may call the API only from {@code takodu.app-url}, the web app. */
@QuarkusTest
class CorsTest {

    @Test
    void webAppMayCallTheApi() {
        preflight("http://localhost:5173").statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:5173"));
    }

    @Test
    void anotherSiteIsRefused() {
        preflight("https://other.example").header("Access-Control-Allow-Origin", nullValue());
    }

    private static ValidatableResponse preflight(String origin) {
        return given().header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,x-organization-id,idempotency-key")
                .options("/api/v1/auth/login-codes").then();
    }
}
