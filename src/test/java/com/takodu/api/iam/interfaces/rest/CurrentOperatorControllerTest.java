package com.takodu.api.iam.interfaces.rest;

import com.takodu.api.shared.testing.TestOperators;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class CurrentOperatorControllerTest {

    @Test
    void operatorIsAdmitted() {
        var operator = TestOperators.operator();

        given().spec(operator.spec()).get("/internal/v1/operator")
                .then().statusCode(200).body("email", equalTo(operator.email()));
    }

    @Test
    void anonymousCallerIsRefused() {
        given().get("/internal/v1/operator").then().statusCode(401);
    }

    @Test
    void memberTokenIsRefusedEvenWithItsOrganization() {
        var owner = TestTenants.owner();

        given().spec(owner.spec()).get("/internal/v1/operator")
                .then().statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void disabledOperatorIsRefused() {
        var operator = TestOperators.operator();
        TestOperators.disable(operator.email());

        given().spec(operator.spec()).get("/internal/v1/operator")
                .then().statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }
}
