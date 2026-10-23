package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestOperators;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class OrganizationAccountsControllerTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void operatorSeesEveryOrganizationWithItsOwnerAndPlan() {
        var owner = TestTenants.owner();
        var account = "find { it.id == '%s' }.".formatted(owner.organizationId());

        given().spec(TestOperators.operator().spec()).get("/internal/v1/organizations")
                .then().statusCode(200)
                .body(account + "plan", equalTo("standard"))
                .body(account + "planActive", equalTo(true))
                .body(account + "ownerEmail", equalTo(owner.email()))
                .body(account + "activeEstablishments", equalTo(1));
    }

    @Test
    void anExpiredTrialIsShownInactive() {
        var owner = TestTenants.owner();
        clock.advance(Duration.ofDays(15));

        given().spec(TestOperators.operator().spec()).get("/internal/v1/organizations")
                .then().statusCode(200)
                .body("find { it.id == '%s' }.planActive".formatted(owner.organizationId()), equalTo(false));
    }

    @Test
    void anonymousCallerIsRefused() {
        given().get("/internal/v1/organizations").then().statusCode(401);
    }

    @Test
    void memberTokenIsRefused() {
        given().spec(TestTenants.owner().spec()).get("/internal/v1/organizations")
                .then().statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }
}
