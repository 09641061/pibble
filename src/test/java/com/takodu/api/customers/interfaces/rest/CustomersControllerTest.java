package com.takodu.api.customers.interfaces.rest;

import com.takodu.api.shared.infrastructure.time.TestClock;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class CustomersControllerTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void staffRegistersACustomerTypedWithoutCountryCode() {
        var staff = TestTenants.staffOf(TestTenants.owner());
        var body = body("987 654 321");
        body.put("name", "Rosa Quispe");
        body.put("documentType", "dni");
        body.put("documentNumber", "45678912");

        register(staff, UUID.randomUUID(), body).statusCode(201)
                .body("phone", equalTo("+51987654321"))
                .body("displayName", equalTo("Rosa Quispe"))
                .body("documentType", equalTo("dni"))
                .body("whatsappProfileName", nullValue());
    }

    @Test
    void retryReturnsTheSameCustomer() {
        var owner = TestTenants.owner();
        var key = UUID.randomUUID();
        String first = register(owner, key, body("987654321")).statusCode(201).extract().path("id");

        register(owner, key, body("987654321")).statusCode(201).body("id", equalTo(first));
    }

    @Test
    void activePhoneIsRegisteredOncePerOrganization() {
        var owner = TestTenants.owner();
        register(owner, UUID.randomUUID(), body("+51 987 654 321")).statusCode(201);

        register(owner, UUID.randomUUID(), body("987654321")).statusCode(409).body("code", equalTo("CUSTOMER_CONFLICT"));
        register(TestTenants.owner(), UUID.randomUUID(), body("987654321")).statusCode(201);
    }

    @Test
    void twoSimultaneousRegistrationsOfOnePhoneCreateOneCustomer() {
        var owner = TestTenants.owner();
        var first = CompletableFuture.supplyAsync(() -> register(owner, UUID.randomUUID(), body("987654321")).extract());
        var second = CompletableFuture.supplyAsync(() -> register(owner, UUID.randomUUID(), body("987654321")).extract());

        var responses = List.of(first.join(), second.join());
        assertEquals(List.of(201, 409), responses.stream().map(r -> r.statusCode()).sorted().toList());
        responses.stream().filter(r -> r.statusCode() == 409)
                .forEach(r -> assertEquals("CUSTOMER_CONFLICT", r.path("code")));
    }

    @Test
    void phoneWithoutCountryCodeIsRefused() {
        register(TestTenants.owner(), UUID.randomUUID(), body("87654321"))
                .statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void documentNeedsItsType() {
        var body = body("987654321");
        body.put("documentNumber", "45678912");

        register(TestTenants.owner(), UUID.randomUUID(), body).statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void registrationNeedsAnIdempotencyKey() {
        given().spec(TestTenants.owner().spec()).contentType(ContentType.JSON).body(body("987654321"))
                .post("/api/v1/customers").then().statusCode(400);
    }

    @Test
    void registrationWorksWhileThePlanIsInactive() {
        var owner = TestTenants.owner();
        clock.advance(Duration.ofDays(15));

        register(owner, UUID.randomUUID(), body("987654321")).statusCode(201);
    }

    @Test
    void nonMemberCannotRegister() {
        var owner = TestTenants.owner();
        given().auth().oauth2(TestTenants.outsider()).header("X-Organization-Id", owner.organizationId().toString())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON).body(body("987654321"))
                .post("/api/v1/customers").then().statusCode(403);
    }

    @Test
    void memberReadsACustomer() {
        var owner = TestTenants.owner();
        String id = register(owner, UUID.randomUUID(), body("987654321")).extract().path("id");

        given().spec(TestTenants.staffOf(owner).spec()).get("/api/v1/customers/" + id)
                .then().statusCode(200).body("displayName", equalTo("+51987654321"));
    }

    @Test
    void customerOfAnotherOrganizationIsNotFound() {
        String id = register(TestTenants.owner(), UUID.randomUUID(), body("987654321")).extract().path("id");

        given().spec(TestTenants.owner().spec()).get("/api/v1/customers/" + id)
                .then().statusCode(404).body("code", equalTo("CUSTOMER_NOT_FOUND"));
    }

    static Map<String, Object> body(String phone) {
        var body = new HashMap<String, Object>();
        body.put("phone", phone);
        return body;
    }

    static ValidatableResponse register(TestTenant tenant, UUID key, Map<String, Object> body) {
        return given().spec(tenant.spec()).header("Idempotency-Key", key.toString())
                .contentType(ContentType.JSON).body(body)
                .post("/api/v1/customers").then();
    }

    static String registered(TestTenant tenant, String phone) {
        return register(tenant, UUID.randomUUID(), body(phone)).statusCode(201).extract().path("id");
    }
}
