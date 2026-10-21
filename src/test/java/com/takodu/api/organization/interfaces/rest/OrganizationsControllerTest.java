package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import com.takodu.api.iam.interfaces.rest.SignIn;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class OrganizationsControllerTest {

    @Inject
    RecordingEmailSender emailSender;

    @Test
    void creatorBecomesOwnerOfATrialOrganization() {
        var token = signIn();
        String id = create(token, UUID.randomUUID(), body("Barbería Ñandú"))
                .statusCode(201)
                .body("name", equalTo("Barbería Ñandú"))
                .body("plan", equalTo("standard"))
                .body("planActiveUntil", notNullValue())
                .extract().path("id");

        given().auth().oauth2(token).get("/api/v1/organizations")
                .then().statusCode(200)
                .body("", hasSize(1))
                .body("[0].id", equalTo(id))
                .body("[0].role", equalTo("owner"));
    }

    @Test
    void retryWithTheSameKeyReturnsTheSameOrganization() {
        var token = signIn();
        var key = UUID.randomUUID();
        String id = create(token, key, body("Spa Sol")).statusCode(201).extract().path("id");

        create(token, key, body("Spa Sol")).statusCode(201).body("id", equalTo(id));
        given().auth().oauth2(token).get("/api/v1/organizations").then().body("", hasSize(1));
    }

    @Test
    void anotherUsersKeyIsRefused() {
        var key = UUID.randomUUID();
        create(signIn(), key, body("Spa Luna")).statusCode(201);

        create(signIn(), key, body("Spa Luna")).statusCode(409).body("code", equalTo("REQUEST_CONFLICT"));
    }

    @Test
    void unknownTimeZoneIsRefused() {
        var body = body("Uñas Bonitas");
        body.put("timeZone", "America/Atlantis");

        create(signIn(), UUID.randomUUID(), body).statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void missingIdempotencyKeyIsRefused() {
        given().auth().oauth2(signIn()).contentType(ContentType.JSON).body(body("Spa"))
                .post("/api/v1/organizations")
                .then().statusCode(400);
    }

    @Test
    void anonymousCallerIsRefused() {
        given().contentType(ContentType.JSON).header("Idempotency-Key", UUID.randomUUID().toString()).body(body("Spa"))
                .post("/api/v1/organizations")
                .then().statusCode(401);
    }

    private String signIn() {
        return SignIn.as(SignIn.newEmail(), emailSender).extract().path("accessToken");
    }

    private static io.restassured.response.ValidatableResponse create(String token, UUID key, Map<String, String> body) {
        return given().auth().oauth2(token).header("Idempotency-Key", key.toString())
                .contentType(ContentType.JSON).body(body)
                .post("/api/v1/organizations")
                .then();
    }

    private static Map<String, String> body(String name) {
        var body = new HashMap<String, String>();
        body.put("name", name);
        body.put("establishmentName", "Sede Centro");
        body.put("ownerDisplayName", "Pepe");
        return body;
    }
}
