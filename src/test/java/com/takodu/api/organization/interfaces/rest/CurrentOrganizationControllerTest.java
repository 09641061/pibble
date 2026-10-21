package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class CurrentOrganizationControllerTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void memberReadsTheOrganizationOfTheHeader() {
        var owner = TestTenants.owner();

        given().spec(owner.spec()).get("/api/v1/organization")
                .then().statusCode(200).body("id", equalTo(owner.organizationId().toString()));
    }

    @Test
    void missingHeaderIsRefused() {
        given().auth().oauth2(TestTenants.owner().accessToken()).get("/api/v1/organization")
                .then().statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void malformedHeaderIsRefused() {
        given().auth().oauth2(TestTenants.owner().accessToken()).header("X-Organization-Id", "nope")
                .get("/api/v1/organization")
                .then().statusCode(400);
    }

    @Test
    void nonMemberIsRefused() {
        var owner = TestTenants.owner();

        given().auth().oauth2(TestTenants.outsider()).header("X-Organization-Id", owner.organizationId().toString())
                .get("/api/v1/organization")
                .then().statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void unknownOrganizationIsRefused() {
        given().auth().oauth2(TestTenants.owner().accessToken()).header("X-Organization-Id", UUID.randomUUID().toString())
                .get("/api/v1/organization")
                .then().statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void anonymousCallerIsRefused() {
        given().header("X-Organization-Id", UUID.randomUUID().toString()).get("/api/v1/organization")
                .then().statusCode(401);
    }

    @Test
    void ownerRenamesTheOrganization() {
        var owner = TestTenants.owner();

        rename(owner.spec(), "Barbería Don Pepe").statusCode(200).body("name", equalTo("Barbería Don Pepe"));
    }

    @Test
    void renamingNeedsAnActivePlan() {
        var owner = TestTenants.owner();
        clock.advance(Duration.ofDays(15));

        rename(owner.spec(), "Later").statusCode(402).body("code", equalTo("PLAN_INACTIVE"));
    }

    private static io.restassured.response.ValidatableResponse rename(io.restassured.specification.RequestSpecification spec,
                                                                      String name) {
        return given().spec(spec).contentType(ContentType.JSON).body(Map.of("name", name))
                .put("/api/v1/organization").then();
    }
}
