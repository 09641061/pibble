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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class EstablishmentServicesControllerTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void managerCreatesAServicePerformedByStaff() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        var staff = TestTenants.staffOf(owner, establishment);

        create(TestTenants.managerOf(owner), establishment, UUID.randomUUID(), body("Corte clásico", List.of(staff.memberId())))
                .statusCode(201)
                .body("name", equalTo("Corte clásico"))
                .body("durationMinutes", equalTo(45))
                .body("priceCents", equalTo(3500))
                .body("currency", equalTo("PEN"))
                .body("active", equalTo(true))
                .body("performerIds", contains(staff.memberId().toString()));

        given().spec(staff.spec()).get(path(establishment)).then().statusCode(200).body("", hasSize(1));
    }

    @Test
    void retryWithTheSameKeyReturnsTheSameService() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        var key = UUID.randomUUID();
        String id = create(owner, establishment, key, body("Manicure", List.of())).statusCode(201).extract().path("id");

        create(owner, establishment, key, body("Manicure", List.of())).statusCode(201).body("id", equalTo(id));
        given().spec(owner.spec()).get(path(establishment)).then().body("", hasSize(1));
    }

    @Test
    void staffCannotCreateServices() {
        var owner = TestTenants.owner();

        create(TestTenants.staffOf(owner), TestTenants.firstEstablishmentOf(owner), UUID.randomUUID(), body("Corte", List.of()))
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void pendingInvitationCannotPerform() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        TestTenants.invite(owner, "user-" + UUID.randomUUID() + "@example.com", "staff", List.of(establishment));
        String pending = given().spec(owner.spec()).get("/api/v1/members")
                .then().extract().path("find { it.status == 'pending' }.id");

        create(owner, establishment, UUID.randomUUID(), body("Corte", List.of(UUID.fromString(pending))))
                .statusCode(422).body("code", equalTo("PERFORMER_NOT_ELIGIBLE"));
    }

    @Test
    void memberOfAnotherOrganizationCannotPerform() {
        var owner = TestTenants.owner();
        var foreign = TestTenants.owner();

        create(owner, TestTenants.firstEstablishmentOf(owner), UUID.randomUUID(), body("Corte", List.of(foreign.memberId())))
                .statusCode(404).body("code", equalTo("MEMBER_NOT_FOUND"));
    }

    @Test
    void zeroMinutesIsRefused() {
        var owner = TestTenants.owner();
        var body = body("Corte", List.of());
        body.put("durationMinutes", 0);

        create(owner, TestTenants.firstEstablishmentOf(owner), UUID.randomUUID(), body).statusCode(400);
    }

    @Test
    void creatingNeedsAnActivePlan() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        clock.advance(Duration.ofDays(15));

        create(owner, establishment, UUID.randomUUID(), body("Corte", List.of())).statusCode(402);
    }

    @Test
    void archivedEstablishmentTakesNoServices() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        EstablishmentsControllerTest.archive(owner, establishment).statusCode(200);

        create(owner, establishment, UUID.randomUUID(), body("Corte", List.of()))
                .statusCode(422).body("code", equalTo("ESTABLISHMENT_ARCHIVED"));
    }

    @Test
    void establishmentOfAnotherOrganizationIsNotFound() {
        var foreign = TestTenants.firstEstablishmentOf(TestTenants.owner());

        given().spec(TestTenants.owner().spec()).get(path(foreign)).then().statusCode(404);
    }

    static String path(UUID establishmentId) {
        return "/api/v1/establishments/" + establishmentId + "/services";
    }

    static ValidatableResponse create(TestTenant tenant, UUID establishmentId, UUID key, Map<String, Object> body) {
        return given().spec(tenant.spec()).header("Idempotency-Key", key.toString()).contentType(ContentType.JSON)
                .body(body).post(path(establishmentId)).then();
    }

    static Map<String, Object> body(String name, List<UUID> performerIds) {
        var body = new HashMap<String, Object>();
        body.put("name", name);
        body.put("durationMinutes", 45);
        body.put("priceCents", 3500);
        body.put("performerIds", performerIds);
        return body;
    }
}
