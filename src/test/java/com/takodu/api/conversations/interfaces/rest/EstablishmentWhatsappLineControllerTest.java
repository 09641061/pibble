package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.RecordingWhatsappOnboarding;
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
import java.util.Map;
import java.util.UUID;

import static com.takodu.api.conversations.TestWhatsapp.connect;
import static com.takodu.api.conversations.TestWhatsapp.newPhoneNumberId;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class EstablishmentWhatsappLineControllerTest {

    @Inject
    TestClock clock;

    @Inject
    RecordingWhatsappOnboarding onboarding;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void ownerConnectsTheLineInCoexistenceMode() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        var phoneNumberId = newPhoneNumberId();

        connect(owner, establishment, UUID.randomUUID(), phoneNumberId)
                .statusCode(201)
                .body("displayPhone", equalTo(RecordingWhatsappOnboarding.DISPLAY_PHONE))
                .body("mode", equalTo("coexistence"))
                .body("status", equalTo("connected"))
                .body(not(containsString("token")));

        get(owner, establishment).statusCode(200).body("status", equalTo("connected"));
    }

    @Test
    void retryWithTheSameKeyReturnsTheLineWithoutASecondSignup() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        var phoneNumberId = newPhoneNumberId();
        var key = UUID.randomUUID();
        String id = connect(owner, establishment, key, phoneNumberId).statusCode(201).extract().path("id");

        connect(owner, establishment, key, phoneNumberId).statusCode(201).body("id", equalTo(id));

        assertEquals(1, onboarding.signupsOf(phoneNumberId));
    }

    @Test
    void onlyTheOwnerConnectsAndDisconnects() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        var manager = TestTenants.managerOf(owner);
        var staff = TestTenants.staffOf(owner);

        connect(manager, establishment, UUID.randomUUID(), newPhoneNumberId()).statusCode(403);
        connect(staff, establishment, UUID.randomUUID(), newPhoneNumberId()).statusCode(403);
        TestWhatsapp.connectLine(owner, establishment);
        disconnect(manager, establishment).statusCode(403).body("code", equalTo("ACCESS_DENIED"));

        get(staff, establishment).statusCode(200);
    }

    @Test
    void staffOfAnotherEstablishmentCannotSeeItsLine() {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();
        var staff = salon.hireStaff();
        TestWhatsapp.connectLine(salon.owner(), surco);

        get(staff, surco).statusCode(403);
    }

    @Test
    void anotherOrganizationsEstablishmentIsNotFound() {
        var owner = TestTenants.owner();
        var foreign = TestTenants.firstEstablishmentOf(TestTenants.owner());

        connect(owner, foreign, UUID.randomUUID(), newPhoneNumberId()).statusCode(404);
        get(owner, foreign).statusCode(404).body("code", equalTo("ESTABLISHMENT_NOT_FOUND"));
        disconnect(owner, foreign).statusCode(404);
    }

    @Test
    void establishmentWithoutALineIsNotFound() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);

        get(owner, establishment).statusCode(404).body("code", equalTo("WHATSAPP_LINE_NOT_FOUND"));
        disconnect(owner, establishment).statusCode(404);
    }

    @Test
    void establishmentKeepsOneConnectedLine() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        var phoneNumberId = TestWhatsapp.connectLine(owner, establishment);

        connect(owner, establishment, UUID.randomUUID(), phoneNumberId)
                .statusCode(409).body("code", equalTo("LINE_ALREADY_CONNECTED"));
        connect(owner, establishment, UUID.randomUUID(), newPhoneNumberId())
                .statusCode(409).body("code", equalTo("LINE_ALREADY_CONNECTED"));
    }

    @Test
    void numberBelongsToOneEstablishmentAcrossOrganizations() {
        var first = TestTenants.owner();
        var phoneNumberId = TestWhatsapp.connectLine(first, TestTenants.firstEstablishmentOf(first));
        var second = TestTenants.owner();

        connect(second, TestTenants.firstEstablishmentOf(second), UUID.randomUUID(), phoneNumberId)
                .statusCode(409).body("code", equalTo("LINE_ALREADY_CONNECTED"));
        assertEquals(1, onboarding.signupsOf(phoneNumberId));
    }

    @Test
    void reconnectingTheSameNumberReusesTheLine() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        var phoneNumberId = newPhoneNumberId();
        String id = connect(owner, establishment, UUID.randomUUID(), phoneNumberId).statusCode(201).extract().path("id");

        disconnect(owner, establishment).statusCode(204);
        get(owner, establishment).body("status", equalTo("disconnected"));
        assertTrue(onboarding.unsubscribed(TestWhatsapp.WABA_ID));

        connect(owner, establishment, UUID.randomUUID(), phoneNumberId).statusCode(201).body("id", equalTo(id));
    }

    @Test
    void aNewNumberAfterDisconnectingIsANewLine() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        String first = connect(owner, establishment, UUID.randomUUID(), newPhoneNumberId()).extract().path("id");
        disconnect(owner, establishment).statusCode(204);

        String second = connect(owner, establishment, UUID.randomUUID(), newPhoneNumberId()).statusCode(201)
                .extract().path("id");

        get(owner, establishment).body("id", equalTo(second)).body("id", not(equalTo(first)));
    }

    @Test
    void inactivePlanRefusesConnecting() {
        var owner = TestTenants.owner();
        clock.advance(Duration.ofDays(15));

        connect(owner, TestTenants.firstEstablishmentOf(owner), UUID.randomUUID(), newPhoneNumberId())
                .statusCode(402).body("code", equalTo("PLAN_INACTIVE"));
    }

    @Test
    void metaRefusingTheSignupIsReported() {
        var owner = TestTenants.owner();
        onboarding.failNextSignup();

        connect(owner, TestTenants.firstEstablishmentOf(owner), UUID.randomUUID(), newPhoneNumberId())
                .statusCode(422).body("code", equalTo("WHATSAPP_SIGNUP_FAILED"));
    }

    @Test
    void cloudApiNeedsAPinAndKeysAreRequired() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);

        given().spec(owner.spec()).header("Idempotency-Key", UUID.randomUUID().toString()).contentType(ContentType.JSON)
                .body(Map.of("code", "c", "wabaId", "1", "phoneNumberId", newPhoneNumberId(), "mode", "cloud_api"))
                .post("/api/v1/establishments/" + establishment + "/whatsapp-line")
                .then().statusCode(400);
        given().spec(owner.spec()).contentType(ContentType.JSON)
                .body(Map.of("code", "c", "wabaId", "1", "phoneNumberId", newPhoneNumberId()))
                .post("/api/v1/establishments/" + establishment + "/whatsapp-line")
                .then().statusCode(400);
    }

    private static ValidatableResponse get(TestTenant tenant, UUID establishmentId) {
        return given().spec(tenant.spec()).get("/api/v1/establishments/" + establishmentId + "/whatsapp-line").then();
    }

    private static ValidatableResponse disconnect(TestTenant tenant, UUID establishmentId) {
        return given().spec(tenant.spec()).delete("/api/v1/establishments/" + establishmentId + "/whatsapp-line").then();
    }
}
