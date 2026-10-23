package com.takodu.api.iam.interfaces.rest;

import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Drives the sign-in endpoints the way the app does, for tests of iam and of later contexts. */
public final class SignIn {
    private SignIn() { }

    public static String newEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    public static void requestCode(String email) {
        given().contentType(ContentType.JSON).body(Map.of("email", email))
                .when().post("/api/v1/auth/login-codes")
                .then().statusCode(202);
    }

    public static ValidatableResponse submit(String email, String code) {
        return given().contentType(ContentType.JSON).body(Map.of("email", email, "code", code))
                .when().post("/api/v1/auth/sessions")
                .then();
    }

    /** Requests a code and signs in with it; returns the tokens response. */
    public static ValidatableResponse as(String email, RecordingEmailSender emailSender) {
        requestCode(email);
        return submit(email, emailSender.lastCodeSentTo(email)).statusCode(201);
    }
}
