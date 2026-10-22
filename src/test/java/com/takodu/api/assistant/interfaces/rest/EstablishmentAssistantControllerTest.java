package com.takodu.api.assistant.interfaces.rest;

import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/** A1: owners and managers turn the assistant on per establishment and tell it about the business. */
@QuarkusTest
class EstablishmentAssistantControllerTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void assistantIsOffUntilConfigured() {
        var owner = TestTenants.owner();

        get(owner, TestTenants.firstEstablishmentOf(owner)).statusCode(200)
                .body("enabled", equalTo(false))
                .body("businessInfo", equalTo(""));
    }

    @Test
    void ownerTurnsItOnAndAManagerRewritesTheBusinessInfo() {
        var owner = TestTenants.owner();
        var establishmentId = TestTenants.firstEstablishmentOf(owner);

        put(owner, establishmentId, true, "  Aceptamos Yape y Plin.  ").statusCode(200)
                .body("enabled", equalTo(true))
                .body("businessInfo", equalTo("Aceptamos Yape y Plin."));
        put(TestTenants.managerOf(owner), establishmentId, true, "Solo efectivo.").statusCode(200);

        get(owner, establishmentId).statusCode(200)
                .body("establishmentId", equalTo(establishmentId.toString()))
                .body("enabled", equalTo(true))
                .body("businessInfo", equalTo("Solo efectivo."));
    }

    @Test
    void staffCannotSeeOrChangeTheAssistant() {
        var salon = TestSalon.open();
        var assigned = salon.hireStaff();
        var elsewhere = TestTenants.staffOf(salon.owner(), salon.openSecondEstablishment());

        get(assigned, salon.establishmentId()).statusCode(403).body("code", equalTo("ACCESS_DENIED"));
        put(assigned, salon.establishmentId(), true, null).statusCode(403);
        put(elsewhere, salon.establishmentId(), true, null).statusCode(403);
    }

    @Test
    void anotherOrganizationsEstablishmentIsNotFound() {
        var owner = TestTenants.owner();
        var other = TestTenants.firstEstablishmentOf(TestTenants.owner());

        get(owner, other).statusCode(404);
        put(owner, other, true, null).statusCode(404);
        put(owner, UUID.randomUUID(), true, null).statusCode(404);
    }

    @Test
    void businessInfoIsLimitedAndEnabledIsRequired() {
        var owner = TestTenants.owner();
        var establishmentId = TestTenants.firstEstablishmentOf(owner);

        put(owner, establishmentId, true, "x".repeat(4001)).statusCode(400);
        given().spec(owner.spec()).contentType(ContentType.JSON).body(Map.of("businessInfo", "Hola"))
                .put("/api/v1/establishments/" + establishmentId + "/assistant").then().statusCode(400);
    }

    @Test
    void settingsCannotChangeWhileThePlanIsInactive() {
        var owner = TestTenants.owner();
        var establishmentId = TestTenants.firstEstablishmentOf(owner);
        clock.advance(Duration.ofDays(15));

        put(owner, establishmentId, true, null).statusCode(402);
        get(owner, establishmentId).statusCode(200);
    }

    private static ValidatableResponse get(TestTenant tenant, UUID establishmentId) {
        return given().spec(tenant.spec()).get("/api/v1/establishments/" + establishmentId + "/assistant").then();
    }

    private static ValidatableResponse put(TestTenant tenant, UUID establishmentId, boolean enabled,
                                           String businessInfo) {
        var body = new HashMap<String, Object>();
        body.put("enabled", enabled);
        body.put("businessInfo", businessInfo);
        return given().spec(tenant.spec()).contentType(ContentType.JSON).body(body)
                .put("/api/v1/establishments/" + establishmentId + "/assistant").then();
    }
}
