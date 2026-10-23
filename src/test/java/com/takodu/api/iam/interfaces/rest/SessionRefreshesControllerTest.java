package com.takodu.api.iam.interfaces.rest;

import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import com.takodu.api.shared.infrastructure.time.TestClock;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class SessionRefreshesControllerTest {

    @Inject
    RecordingEmailSender emailSender;

    @Inject
    TestClock clock;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void fixClock() {
        clock.set(Instant.now());
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private static ValidatableResponse refresh(String refreshToken) {
        return given().contentType(ContentType.JSON).body(Map.of("refreshToken", refreshToken))
                .when().post("/api/v1/auth/session-refreshes")
                .then();
    }

    private String signIn(String email) {
        return SignIn.as(email, emailSender).extract().path("refreshToken");
    }

    @Test
    void refreshReturnsANewRefreshTokenForTheSameSession() {
        var signedIn = SignIn.as(SignIn.newEmail(), emailSender).extract().jsonPath();
        var first = signedIn.getString("refreshToken");

        refresh(first).statusCode(200)
                .body("sessionId", equalTo(signedIn.getString("sessionId")))
                .body("refreshToken", not(equalTo(first)));
    }

    @Test
    void lostResponseCanBeRetriedWithThePreviousToken() {
        var first = signIn(SignIn.newEmail());
        refresh(first).statusCode(200);
        clock.advance(Duration.ofMinutes(1));

        String retried = refresh(first).statusCode(200).extract().path("refreshToken");

        refresh(retried).statusCode(200);
    }

    @Test
    void previousTokenReusedAfterTheWindowEndsTheSession() {
        var first = signIn(SignIn.newEmail());
        String second = refresh(first).statusCode(200).extract().path("refreshToken");
        clock.advance(Duration.ofMinutes(3));

        refresh(first).statusCode(401).body("code", equalTo("REFRESH_TOKEN_INVALID"));
        refresh(second).statusCode(401);
    }

    @Test
    void sessionExpiresAfterThirtyDaysWithoutUse() {
        var first = signIn(SignIn.newEmail());
        clock.advance(Duration.ofDays(30));

        refresh(first).statusCode(401);
    }

    @Test
    void signingInOnASecondDeviceKeepsTheFirstSession() {
        var email = SignIn.newEmail();
        var phone = signIn(email);
        var computer = signIn(email);

        refresh(phone).statusCode(200);
        refresh(computer).statusCode(200);
    }

    @Test
    void disabledUserCannotRefresh() {
        var email = SignIn.newEmail();
        var token = signIn(email);
        QuarkusTransaction.requiringNew().run(() -> entityManager
                .createNativeQuery("update users set disabled_at = now() where email = :email")
                .setParameter("email", email).executeUpdate());

        refresh(token).statusCode(403);
    }

    @Test
    void unknownTokenIsRefused() {
        refresh("not-a-token").statusCode(401);
    }
}
