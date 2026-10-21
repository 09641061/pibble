package com.takodu.api.iam.interfaces.rest;

import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class LoginCodesControllerTest {

    @Inject
    RecordingEmailSender emailSender;

    static String newEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    static void requestCode(String email, int expectedStatus) {
        given().contentType(ContentType.JSON).body(Map.of("email", email))
                .when().post("/api/v1/auth/login-codes")
                .then().statusCode(expectedStatus);
    }

    @Test
    void requestingACodeEmailsSixDigits() {
        var email = newEmail();

        requestCode(email, 202);

        assertTrue(emailSender.lastCodeSentTo(email).matches("\\d{6}"));
    }

    @Test
    void codeGoesToTheNormalizedAddress() {
        var email = newEmail();

        requestCode("  " + email.toUpperCase() + " ", 202);

        assertEquals(1, emailSender.codesSentTo(email).size());
    }

    @Test
    void sixthCodeWithinAnHourIsRefused() {
        var email = newEmail();
        for (int i = 0; i < 5; i++) requestCode(email, 202);

        given().contentType(ContentType.JSON).body(Map.of("email", email))
                .when().post("/api/v1/auth/login-codes")
                .then().statusCode(429).body("code", equalTo("LOGIN_CODE_LIMIT"));
        assertEquals(5, emailSender.codesSentTo(email).size());
    }

    @Test
    void malformedEmailIsRefused() {
        given().contentType(ContentType.JSON).body(Map.of("email", "ana@gmail"))
                .when().post("/api/v1/auth/login-codes")
                .then().statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }
}
