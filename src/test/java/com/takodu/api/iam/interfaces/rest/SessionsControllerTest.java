package com.takodu.api.iam.interfaces.rest;

import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import com.takodu.api.shared.infrastructure.time.TestClock;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class SessionsControllerTest {

    @Inject
    RecordingEmailSender emailSender;

    @Inject
    TestClock clock;

    @Inject
    JWTParser jwtParser;

    @Inject
    EntityManager entityManager;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void firstSignInCreatesTheUserAndReturnsTokens() throws Exception {
        var email = SignIn.newEmail();

        var body = SignIn.as(email, emailSender)
                .body("user.email", equalTo(email))
                .body("user.name", nullValue())
                .body("refreshToken", notNullValue())
                .extract().jsonPath();

        var token = jwtParser.parse(body.getString("accessToken"));
        assertEquals(body.getString("user.id"), token.getSubject());
        assertEquals(body.getString("sessionId"), token.<String>getClaim("sid"));
    }

    @Test
    void secondSignInReusesTheUser() {
        var email = SignIn.newEmail();
        var first = SignIn.as(email, emailSender).extract().path("user.id");

        SignIn.as(email, emailSender).body("user.id", equalTo(first));
    }

    @Test
    void wrongCodeIsRefused() {
        var email = SignIn.newEmail();
        SignIn.requestCode(email);

        SignIn.submit(email, wrong(emailSender.lastCodeSentTo(email))).statusCode(401).body("code", equalTo("LOGIN_CODE_INVALID"));
    }

    @Test
    void codeWorksOnlyOnce() {
        var email = SignIn.newEmail();
        SignIn.requestCode(email);
        var code = emailSender.lastCodeSentTo(email);
        SignIn.submit(email, code).statusCode(201);

        SignIn.submit(email, code).statusCode(401);
    }

    @Test
    void codeExpiresAfterTenMinutes() {
        var email = SignIn.newEmail();
        clock.set(Instant.now());
        SignIn.requestCode(email);
        clock.advance(Duration.ofMinutes(10));

        SignIn.submit(email, emailSender.lastCodeSentTo(email)).statusCode(401);
    }

    @Test
    void fiveWrongAttemptsBlockTheRightCode() {
        var email = SignIn.newEmail();
        SignIn.requestCode(email);
        var code = emailSender.lastCodeSentTo(email);
        for (int i = 0; i < 5; i++) SignIn.submit(email, wrong(code)).statusCode(401);

        SignIn.submit(email, code).statusCode(401);
    }

    @Test
    void olderCodeStillWorksAndConsumesTheNewerOne() {
        var email = SignIn.newEmail();
        SignIn.requestCode(email);
        var older = emailSender.lastCodeSentTo(email);
        SignIn.requestCode(email);
        var newer = emailSender.lastCodeSentTo(email);

        SignIn.submit(email, older).statusCode(201);
        SignIn.submit(email, newer).statusCode(401);
    }

    @Test
    void disabledUserCannotSignIn() {
        var email = SignIn.newEmail();
        SignIn.as(email, emailSender);
        QuarkusTransaction.requiringNew().run(() -> entityManager
                .createNativeQuery("update users set disabled_at = now() where email = :email")
                .setParameter("email", email).executeUpdate());

        SignIn.requestCode(email);
        SignIn.submit(email, emailSender.lastCodeSentTo(email)).statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void codeThatIsNotSixDigitsIsInvalidInput() {
        SignIn.submit(SignIn.newEmail(), "12345").statusCode(400);
    }

    @Test
    void sessionListShowsEveryDeviceAndMarksTheCallingOne() {
        var email = SignIn.newEmail();
        var phone = SignIn.as(email, emailSender).extract().jsonPath();
        var computer = SignIn.as(email, emailSender).extract().jsonPath();

        given().auth().oauth2(computer.getString("accessToken"))
                .when().get("/api/v1/auth/sessions")
                .then().statusCode(200)
                .body("id", containsInAnyOrder(phone.getString("sessionId"), computer.getString("sessionId")))
                .body("find { it.current }.id", equalTo(computer.getString("sessionId")));
    }

    @Test
    void revokingADeviceEndsItsSessionOnly() {
        var email = SignIn.newEmail();
        var phone = SignIn.as(email, emailSender).extract().jsonPath();
        var computer = SignIn.as(email, emailSender).extract().jsonPath();

        given().auth().oauth2(computer.getString("accessToken"))
                .when().delete("/api/v1/auth/sessions/" + phone.getString("sessionId"))
                .then().statusCode(204);

        given().contentType("application/json").body(Map.of("refreshToken", phone.getString("refreshToken")))
                .when().post("/api/v1/auth/session-refreshes").then().statusCode(401);
        given().auth().oauth2(computer.getString("accessToken"))
                .when().get("/api/v1/auth/sessions")
                .then().body("id", contains(computer.getString("sessionId")));
    }

    @Test
    void anotherUsersSessionIsNotFound() {
        var mine = SignIn.as(SignIn.newEmail(), emailSender).extract().jsonPath();
        var theirs = SignIn.as(SignIn.newEmail(), emailSender).extract().jsonPath();

        given().auth().oauth2(mine.getString("accessToken"))
                .when().delete("/api/v1/auth/sessions/" + theirs.getString("sessionId"))
                .then().statusCode(404).body("code", equalTo("SESSION_NOT_FOUND"));
        given().auth().oauth2(theirs.getString("accessToken"))
                .when().get("/api/v1/auth/sessions")
                .then().body("$", hasSize(1));
    }

    @Test
    void sessionEndpointsRequireAnAccessToken() {
        given().when().get("/api/v1/auth/sessions").then().statusCode(401);
        given().when().delete("/api/v1/auth/sessions/" + UUID.randomUUID()).then().statusCode(401);
    }

    private static String wrong(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }
}
