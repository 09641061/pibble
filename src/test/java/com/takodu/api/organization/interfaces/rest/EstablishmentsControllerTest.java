package com.takodu.api.organization.interfaces.rest;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class EstablishmentsControllerTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void ownerSeesTheFirstEstablishment() {
        var owner = TestTenants.owner();

        given().spec(owner.spec()).get("/api/v1/establishments")
                .then().statusCode(200)
                .body("", hasSize(1))
                .body("[0].name", equalTo("Main branch"))
                .body("[0].timeZone", equalTo("America/Lima"));
    }

    @Test
    void standardPlanOpensNoSecondEstablishment() {
        open(TestTenants.owner(), UUID.randomUUID(), "Sede Surco")
                .statusCode(422).body("code", equalTo("PLAN_LIMIT_REACHED"));
    }

    @Test
    void archivingFreesThePlaceAndARetryReturnsTheSameEstablishment() {
        var owner = TestTenants.owner();
        archive(owner, TestTenants.firstEstablishmentOf(owner)).statusCode(200).body("archivedAt", notNullValue());
        var key = UUID.randomUUID();

        String id = open(owner, key, "Sede Surco").statusCode(201).extract().path("id");

        open(owner, key, "Sede Surco").statusCode(201).body("id", equalTo(id));
        given().spec(owner.spec()).get("/api/v1/establishments").then().body("", hasSize(1)).body("[0].id", equalTo(id));
    }

    @Test
    void twoSimultaneousOpeningsTakeTheLastPlaceOnce() {
        var owner = TestTenants.owner();
        archive(owner, TestTenants.firstEstablishmentOf(owner)).statusCode(200);

        var first = CompletableFuture.supplyAsync(() -> open(owner, UUID.randomUUID(), "A").extract().statusCode());
        var second = CompletableFuture.supplyAsync(() -> open(owner, UUID.randomUUID(), "B").extract().statusCode());

        assertEquals(List.of(201, 422), List.of(first.join(), second.join()).stream().sorted().toList());
    }

    @Test
    void openingNeedsAnActivePlan() {
        var owner = TestTenants.owner();
        archive(owner, TestTenants.firstEstablishmentOf(owner)).statusCode(200);
        clock.advance(Duration.ofDays(15));

        open(owner, UUID.randomUUID(), "Sede Surco").statusCode(402).body("code", equalTo("PLAN_INACTIVE"));
    }

    @Test
    void archivingWorksWhileThePlanIsInactive() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        clock.advance(Duration.ofDays(15));

        archive(owner, establishment).statusCode(200);
    }

    @Test
    void ownerChangesTheTimeZone() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);

        describe(owner, establishment, Map.of("name", "Sede Bogotá", "timeZone", "America/Bogota"))
                .statusCode(200).body("timeZone", equalTo("America/Bogota")).body("name", equalTo("Sede Bogotá"));
    }

    @Test
    void unknownTimeZoneIsRefused() {
        var owner = TestTenants.owner();

        describe(owner, TestTenants.firstEstablishmentOf(owner), Map.of("name", "Sede", "timeZone", "Lima"))
                .statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void archivedEstablishmentTakesNoChanges() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        archive(owner, establishment).statusCode(200);

        describe(owner, establishment, Map.of("name", "Sede"))
                .statusCode(422).body("code", equalTo("ESTABLISHMENT_ARCHIVED"));
    }

    @Test
    void maxPlanOpensMoreEstablishments() {
        var owner = TestTenants.owner();
        TestTenants.upgradeToMax(owner);

        open(owner, UUID.randomUUID(), "Sede Surco").statusCode(201);
        open(owner, UUID.randomUUID(), "Sede Barranco").statusCode(201);
        given().spec(owner.spec()).get("/api/v1/establishments").then().body("", hasSize(3));
    }

    @Test
    void staffSeeOnlyTheirEstablishments() {
        var owner = TestTenants.owner();
        TestTenants.upgradeToMax(owner);
        var main = TestTenants.firstEstablishmentOf(owner);
        String other = open(owner, UUID.randomUUID(), "Sede Surco").statusCode(201).extract().path("id");
        var staff = TestTenants.staffOf(owner, UUID.fromString(other));

        given().spec(staff.spec()).get("/api/v1/establishments")
                .then().statusCode(200).body("", hasSize(1)).body("[0].id", equalTo(other));
        given().spec(staff.spec()).get("/api/v1/establishments/" + main)
                .then().statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void managerCannotOpenEstablishments() {
        var owner = TestTenants.owner();
        TestTenants.upgradeToMax(owner);

        open(TestTenants.managerOf(owner), UUID.randomUUID(), "Sede Surco")
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void establishmentOfAnotherOrganizationIsNotFound() {
        var other = TestTenants.firstEstablishmentOf(TestTenants.owner());

        given().spec(TestTenants.owner().spec()).get("/api/v1/establishments/" + other)
                .then().statusCode(404).body("code", equalTo("ESTABLISHMENT_NOT_FOUND"));
    }

    static ValidatableResponse open(TestTenant tenant, UUID key, String name) {
        return given().spec(tenant.spec()).header("Idempotency-Key", key.toString()).contentType(ContentType.JSON)
                .body(Map.of("name", name)).post("/api/v1/establishments").then();
    }

    static ValidatableResponse archive(TestTenant tenant, UUID establishmentId) {
        return given().spec(tenant.spec()).post("/api/v1/establishments/" + establishmentId + "/archivals").then();
    }

    private static ValidatableResponse describe(TestTenant tenant, UUID establishmentId, Map<String, String> body) {
        return given().spec(tenant.spec()).contentType(ContentType.JSON).body(body)
                .put("/api/v1/establishments/" + establishmentId).then();
    }
}
