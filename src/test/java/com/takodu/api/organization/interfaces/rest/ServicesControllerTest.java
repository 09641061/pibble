package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.shared.testing.TestTenant;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class ServicesControllerTest {

    @Test
    void ownerRepricesAService() {
        var owner = TestTenants.owner();
        var service = createService(owner, List.of());
        var body = EstablishmentServicesControllerTest.body("Corte premium", List.of(owner.memberId()));
        body.put("priceCents", 5000);

        given().spec(owner.spec()).contentType(ContentType.JSON).body(body).put("/api/v1/services/" + service)
                .then().statusCode(200)
                .body("name", equalTo("Corte premium"))
                .body("priceCents", equalTo(5000))
                .body("performerIds", contains(owner.memberId().toString()));
    }

    @Test
    void deactivatedServiceIsOfferedAgain() {
        var owner = TestTenants.owner();
        var service = createService(owner, List.of());

        given().spec(owner.spec()).post("/api/v1/services/" + service + "/deactivations")
                .then().statusCode(200).body("active", equalTo(false));
        given().spec(owner.spec()).post("/api/v1/services/" + service + "/deactivations")
                .then().statusCode(200).body("active", equalTo(false));
        given().spec(owner.spec()).post("/api/v1/services/" + service + "/activations")
                .then().statusCode(200).body("active", equalTo(true));
    }

    @Test
    void removedMemberStopsPerforming() {
        var owner = TestTenants.owner();
        var staff = TestTenants.staffOf(owner);
        var service = createService(owner, List.of(staff.memberId()));

        given().spec(owner.spec()).delete("/api/v1/members/" + staff.memberId()).then().statusCode(204);

        given().spec(owner.spec()).get("/api/v1/services/" + service).then().body("performerIds", empty());
    }

    @Test
    void memberMarkedNotBookableStopsPerforming() {
        var owner = TestTenants.owner();
        var manager = TestTenants.managerOf(owner);
        var service = createService(owner, List.of(manager.memberId()));

        given().spec(owner.spec()).contentType(ContentType.JSON)
                .body(Map.of("displayName", "Lucía", "bookable", false, "role", "manager", "establishmentIds", List.of()))
                .put("/api/v1/members/" + manager.memberId()).then().statusCode(200);

        given().spec(owner.spec()).get("/api/v1/services/" + service).then().body("performerIds", empty());
    }

    @Test
    void staffOfAnotherEstablishmentCannotReadTheService() {
        var owner = TestTenants.owner();
        TestTenants.upgradeToMax(owner);
        var service = createService(owner, List.of());
        String other = EstablishmentsControllerTest.open(owner, UUID.randomUUID(), "Sede Surco").extract().path("id");
        var staff = TestTenants.staffOf(owner, UUID.fromString(other));

        given().spec(staff.spec()).get("/api/v1/services/" + service).then().statusCode(403);
    }

    @Test
    void serviceOfAnotherOrganizationIsNotFound() {
        var foreign = createService(TestTenants.owner(), List.of());

        given().spec(TestTenants.owner().spec()).post("/api/v1/services/" + foreign + "/deactivations")
                .then().statusCode(404).body("code", equalTo("SERVICE_NOT_FOUND"));
    }

    private static String createService(TestTenant owner, List<UUID> performerIds) {
        return EstablishmentServicesControllerTest.create(owner, TestTenants.firstEstablishmentOf(owner), UUID.randomUUID(),
                EstablishmentServicesControllerTest.body("Corte", performerIds)).statusCode(201).extract().path("id");
    }
}
