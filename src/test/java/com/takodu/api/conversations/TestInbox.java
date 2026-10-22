package com.takodu.api.conversations;

import com.takodu.api.shared.testing.TestTenant;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Builds inbox state the way production does: a line through the endpoint, customers through the webhook. */
public final class TestInbox {
    private TestInbox() { }

    /**
     * @param line the phone number id of a connected line of the establishment
     * @param at when the customer wrote, which opens the window until 24 hours later
     * @return the conversation the customer's message landed in
     */
    public static UUID customerWrites(TestTenant viewer, UUID establishmentId, String line, String waId, Instant at) {
        MetaWebhooks.post(MetaWebhooks.payload("text-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId,
                "WAMID", MetaWebhooks.newWamid(), "TIMESTAMP", String.valueOf(at.getEpochSecond())))).statusCode(200);
        String id = given().spec(viewer.spec()).get("/api/v1/establishments/" + establishmentId + "/conversations")
                .then().statusCode(200)
                .extract().path("items.find { it.customerPhone == '+%s' }.id".formatted(waId));
        return UUID.fromString(id);
    }

    public static ValidatableResponse reply(TestTenant tenant, UUID conversationId, UUID idempotencyKey, String body) {
        return given().spec(tenant.spec()).header("Idempotency-Key", idempotencyKey.toString())
                .contentType(ContentType.JSON).body(Map.of("body", body))
                .post("/api/v1/conversations/" + conversationId + "/messages").then();
    }

    public static ValidatableResponse messages(TestTenant tenant, UUID conversationId) {
        return given().spec(tenant.spec()).get("/api/v1/conversations/" + conversationId + "/messages").then();
    }

    public static ValidatableResponse conversation(TestTenant tenant, UUID conversationId) {
        return given().spec(tenant.spec()).get("/api/v1/conversations/" + conversationId).then();
    }
}
