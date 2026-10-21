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

import static org.hamcrest.Matchers.equalTo;
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

    private static String wrong(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }
}
