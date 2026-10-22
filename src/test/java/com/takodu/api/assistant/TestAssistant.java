package com.takodu.api.assistant;

import com.takodu.api.shared.testing.TestTenant;
import io.restassured.http.ContentType;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Configures the assistant through its endpoint, as an owner does. */
public final class TestAssistant {
    private TestAssistant() { }

    public static void enable(TestTenant owner, UUID establishmentId, String businessInfo) {
        configure(owner, establishmentId, true, businessInfo);
    }

    public static void disable(TestTenant owner, UUID establishmentId) {
        configure(owner, establishmentId, false, "");
    }

    private static void configure(TestTenant owner, UUID establishmentId, boolean enabled, String businessInfo) {
        given().spec(owner.spec()).contentType(ContentType.JSON)
                .body(Map.of("enabled", enabled, "businessInfo", businessInfo))
                .put("/api/v1/establishments/" + establishmentId + "/assistant").then().statusCode(200);
    }
}
