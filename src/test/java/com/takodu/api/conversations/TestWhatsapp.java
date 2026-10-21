package com.takodu.api.conversations;

import com.takodu.api.shared.testing.TestTenant;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;

/** Connects WhatsApp lines through the endpoint, with Meta replaced by the recording fakes. */
public final class TestWhatsapp {
    public static final String WABA_ID = "102290129340398";

    private TestWhatsapp() { }

    /** @return a random Meta phone number id, unique per test */
    public static String newPhoneNumberId() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000_000L, 999_999_999_999_999L));
    }

    /** @return the phone number id of a new line connected by the owner */
    public static String connectLine(TestTenant owner, UUID establishmentId) {
        var phoneNumberId = newPhoneNumberId();
        connect(owner, establishmentId, UUID.randomUUID(), phoneNumberId).statusCode(201);
        return phoneNumberId;
    }

    public static ValidatableResponse connect(TestTenant tenant, UUID establishmentId, UUID idempotencyKey,
                                              String phoneNumberId) {
        return given().spec(tenant.spec()).header("Idempotency-Key", idempotencyKey.toString())
                .contentType(ContentType.JSON)
                .body(Map.of("code", "AQB-signup-code", "wabaId", WABA_ID, "phoneNumberId", phoneNumberId))
                .post("/api/v1/establishments/" + establishmentId + "/whatsapp-line")
                .then();
    }
}
