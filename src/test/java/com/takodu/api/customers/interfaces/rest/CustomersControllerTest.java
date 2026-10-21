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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@QuarkusTest
public class CustomersControllerTest {

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

    @Test
    void staffEditsACustomerAndLeavingOutAFieldClearsIt() {
        var owner = TestTenants.owner();
        var staff = TestTenants.staffOf(owner);
        var id = registered(owner, "987654321");

        update(staff, id, Map.of("name", "Rosa Quispe", "documentType", "ruc", "documentNumber", "10456789123",
                "notes", "Alérgica al tinte")).statusCode(200)
                .body("displayName", equalTo("Rosa Quispe"))
                .body("documentNumber", equalTo("10456789123"));
        update(staff, id, Map.of("name", "Rosa Quispe")).statusCode(200)
                .body("documentType", nullValue())
                .body("notes", nullValue())
                .body("phone", equalTo("+51987654321"));
    }

    @Test
    void invalidDocumentIsRefusedOnEdit() {
        var owner = TestTenants.owner();

        update(owner, registered(owner, "987654321"), Map.of("documentType", "dni", "documentNumber", "123"))
                .statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void customerOfAnotherOrganizationCannotBeEdited() {
        var id = registered(TestTenants.owner(), "987654321");

        update(TestTenants.owner(), id, Map.of("name", "Rosa")).statusCode(404).body("code", equalTo("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void staffCannotDeleteACustomer() {
        var owner = TestTenants.owner();

        delete(TestTenants.staffOf(owner), registered(owner, "987654321")).statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void managerDeletesACustomerAndThePhoneIsFreeAgain() {
        var owner = TestTenants.owner();
        var manager = TestTenants.managerOf(owner);
        var id = registered(owner, "987654321");

        delete(manager, id).statusCode(204);

        given().spec(owner.spec()).get("/api/v1/customers/" + id).then().statusCode(404);
        update(owner, id, Map.of("name", "Rosa")).statusCode(404);
        delete(owner, id).statusCode(404);
        String again = register(owner, UUID.randomUUID(), body("987654321")).statusCode(201).extract().path("id");
        assertNotEquals(id, again);
    }

    @Test
    void customerOfAnotherOrganizationCannotBeDeleted() {
        var id = registered(TestTenants.owner(), "987654321");

        delete(TestTenants.owner(), id).statusCode(404);
    }

    @Test
    void searchMatchesNameWhatsappNameOrTypedDigits() {
        var owner = TestTenants.owner();
        var rosa = body("987654321");
        rosa.put("name", "Rosa Quispe");
        register(owner, UUID.randomUUID(), rosa).statusCode(201);
        var ana = body("+57 300 123 4567");
        ana.put("name", "Ana 100% real");
        register(owner, UUID.randomUUID(), ana).statusCode(201);
        registered(owner, "912345678");

        search(owner, Map.of("q", "quispe")).body("items.displayName", contains("Rosa Quispe"));
        search(owner, Map.of("q", "987 654")).body("items.displayName", contains("Rosa Quispe"));
        search(owner, Map.of("q", "100%")).body("items.displayName", contains("Ana 100% real"));
        search(owner, Map.of("q", "1_0")).body("items", hasSize(0));
        search(owner, Map.of()).body("items.displayName", contains("+51912345678", "Ana 100% real", "Rosa Quispe"));
    }

    @Test
    void searchPagesAndSkipsDeletedCustomers() {
        var owner = TestTenants.owner();
        registered(owner, "911111111");
        registered(owner, "922222222");
        delete(owner, registered(owner, "933333333")).statusCode(204);

        search(owner, Map.of("size", 1)).body("items.phone", contains("+51911111111")).body("hasMore", equalTo(true));
        search(owner, Map.of("size", 1, "page", 1)).body("items.phone", contains("+51922222222")).body("hasMore", equalTo(false));
        search(owner, Map.of("size", 101)).statusCode(400);
    }

    @Test
    void searchSeesOnlyTheCallersOrganization() {
        registered(TestTenants.owner(), "987654321");

        search(TestTenants.staffOf(TestTenants.owner()), Map.of("q", "987")).body("items", hasSize(0));
    }

    public static Map<String, Object> body(String phone) {
        var body = new HashMap<String, Object>();
        body.put("phone", phone);
        return body;
    }

    public static ValidatableResponse register(TestTenant tenant, UUID key, Map<String, Object> body) {
        return given().spec(tenant.spec()).header("Idempotency-Key", key.toString())
                .contentType(ContentType.JSON).body(body)
                .post("/api/v1/customers").then();
    }

    public static ValidatableResponse search(TestTenant tenant, Map<String, ?> params) {
        return given().spec(tenant.spec()).queryParams(params).get("/api/v1/customers").then();
    }

    public static ValidatableResponse update(TestTenant tenant, String id, Map<String, Object> body) {
        return given().spec(tenant.spec()).contentType(ContentType.JSON).body(body)
                .put("/api/v1/customers/" + id).then();
    }

    public static ValidatableResponse delete(TestTenant tenant, String id) {
        return given().spec(tenant.spec()).delete("/api/v1/customers/" + id).then();
    }

    public static String registered(TestTenant tenant, String phone) {
        return register(tenant, UUID.randomUUID(), body(phone)).statusCode(201).extract().path("id");
    }
}
