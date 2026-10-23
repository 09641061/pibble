package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.booking.interfaces.rest.AppointmentsControllerTest;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestOperator;
import com.takodu.api.shared.testing.TestOperators;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class PlanChangesControllerTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void extendingAnEndedPlanReactivatesBooking() {
        var salon = TestSalon.open();
        clock.advance(Duration.ofDays(15));
        var start = TestSalon.at(clock, 1, "10:00");
        AppointmentsControllerTest.book(salon.owner(), UUID.randomUUID(), AppointmentsControllerTest.body(salon, null, start))
                .statusCode(402);

        change(TestOperators.operator(), salon.owner(), UUID.randomUUID(), Map.of("extendDays", 30, "note", "Yape 2026-10-23"))
                .statusCode(201)
                .body("fromPlan", equalTo("standard"))
                .body("toActiveUntil", equalTo(clock.instant().plus(Duration.ofDays(30)).toString()));

        AppointmentsControllerTest.book(salon.owner(), UUID.randomUUID(), AppointmentsControllerTest.body(salon, null, start))
                .statusCode(201);
    }

    @Test
    void aRepeatedKeyExtendsOnceFromTheCurrentEnd() {
        var owner = TestTenants.owner();
        var operator = TestOperators.operator();
        var trialEnd = Instant.parse(planActiveUntil(owner));
        var key = UUID.randomUUID();
        var body = Map.<String, Object>of("extendDays", 30, "note", "Plin 2026-10-23");

        String first = change(operator, owner, key, body).statusCode(201).extract().path("id");
        String second = change(operator, owner, key, body).statusCode(201).extract().path("id");

        assertEquals(first, second);
        assertEquals(trialEnd.plus(Duration.ofDays(30)).toString(), planActiveUntil(owner));
        history(operator, owner).body("", hasSize(1));
    }

    @Test
    void theHistorySaysWhoChangedWhatAndWhy() {
        var owner = TestTenants.owner();
        var operator = TestOperators.operator();
        var until = clock.instant().plus(Duration.ofDays(60)).truncatedTo(ChronoUnit.SECONDS);
        change(operator, owner, UUID.randomUUID(), Map.of("plan", "max", "activeUntil", until.toString(),
                "note", "Bank transfer 2026-10-23")).statusCode(201);

        history(operator, owner).statusCode(200)
                .body("[0].fromPlan", equalTo("standard"))
                .body("[0].toPlan", equalTo("max"))
                .body("[0].toActiveUntil", equalTo(until.toString()))
                .body("[0].note", equalTo("Bank transfer 2026-10-23"))
                .body("[0].changedByEmail", equalTo(operator.email()));
        given().spec(owner.spec()).get("/api/v1/organization").then().body("plan", equalTo("max"));
    }

    @Test
    void aDowngradeKeepsOpenEstablishmentsButOpensNoNewOne() {
        var salon = TestSalon.open();
        var second = salon.openSecondEstablishment();

        change(TestOperators.operator(), salon.owner(), UUID.randomUUID(), Map.of("plan", "standard", "note", "Back to one branch"))
                .statusCode(201);

        given().spec(salon.owner().spec()).get("/api/v1/establishments").then().body("", hasSize(2));
        EstablishmentsControllerTest.archive(salon.owner(), second).statusCode(200);
        EstablishmentsControllerTest.open(salon.owner(), UUID.randomUUID(), "Sede Miraflores")
                .statusCode(422).body("code", equalTo("PLAN_LIMIT_REACHED"));
    }

    @Test
    void aPastEndStopsThePlan() {
        var owner = TestTenants.owner();

        change(TestOperators.operator(), owner, UUID.randomUUID(),
                Map.of("activeUntil", clock.instant().minusSeconds(1).toString(), "note", "Payment reversed"))
                .statusCode(201);

        given().spec(owner.spec()).contentType(ContentType.JSON).body(Map.of("name", "Later"))
                .put("/api/v1/organization").then().statusCode(402);
    }

    @Test
    void bothEndsOrNoChangeAreRefused() {
        var owner = TestTenants.owner();
        var operator = TestOperators.operator();
        var both = new HashMap<String, Object>(Map.of("extendDays", 30, "note", "x"));
        both.put("activeUntil", clock.instant().toString());

        change(operator, owner, UUID.randomUUID(), both).statusCode(400);
        change(operator, owner, UUID.randomUUID(), Map.of("note", "nothing")).statusCode(400);
        change(operator, owner, UUID.randomUUID(), Map.of("extendDays", 30)).statusCode(400);
        given().spec(operator.spec()).contentType(ContentType.JSON).body(Map.of("extendDays", 30, "note", "no key"))
                .post(path(owner.organizationId())).then().statusCode(400);
    }

    @Test
    void anUnknownOrganizationIsNotFound() {
        var operator = TestOperators.operator();
        var unknown = UUID.randomUUID();

        given().spec(operator.spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON).body(Map.of("extendDays", 30, "note", "Yape"))
                .post(path(unknown)).then().statusCode(404);
        given().spec(operator.spec()).get(path(unknown)).then().statusCode(404);
    }

    @Test
    void membersAndAnonymousCallersCannotChangePlans() {
        var owner = TestTenants.owner();

        given().spec(owner.spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON).body(Map.of("extendDays", 365, "note", "free year"))
                .post(path(owner.organizationId())).then().statusCode(403).body("code", equalTo("ACCESS_DENIED"));
        given().get(path(owner.organizationId())).then().statusCode(401);
    }

    @Test
    void theHistoryCannotBeRewritten() {
        var owner = TestTenants.owner();
        change(TestOperators.operator(), owner, UUID.randomUUID(), Map.of("extendDays", 30, "note", "Yape")).statusCode(201);

        assertThrows(RuntimeException.class, () -> QuarkusTransaction.requiringNew().run(() ->
                Arc.container().instance(EntityManager.class).get()
                        .createNativeQuery("update plan_changes set note = 'edited' where organization_id = :id")
                        .setParameter("id", owner.organizationId()).executeUpdate()));
    }

    private static ValidatableResponse change(TestOperator operator, TestTenant tenant, UUID key, Map<String, Object> body) {
        return given().spec(operator.spec()).header("Idempotency-Key", key.toString()).contentType(ContentType.JSON)
                .body(body).post(path(tenant.organizationId())).then();
    }

    private static ValidatableResponse history(TestOperator operator, TestTenant tenant) {
        return given().spec(operator.spec()).get(path(tenant.organizationId())).then();
    }

    private static String planActiveUntil(TestTenant tenant) {
        return given().spec(tenant.spec()).get("/api/v1/organization").then().statusCode(200)
                .extract().path("planActiveUntil");
    }

    private static String path(UUID organizationId) {
        return "/internal/v1/organizations/" + organizationId + "/plan-changes";
    }
}
