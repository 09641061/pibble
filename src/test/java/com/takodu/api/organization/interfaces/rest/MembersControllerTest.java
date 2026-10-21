package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import com.takodu.api.iam.interfaces.rest.SignIn;
import com.takodu.api.organization.application.internal.outboundservices.email.RecordingInvitationSender;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class MembersControllerTest {

    @Inject
    RecordingInvitationSender invitationSender;

    @Inject
    RecordingEmailSender emailSender;

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void invitedManagerJoinsTheOrganization() {
        var owner = TestTenants.owner();
        var email = SignIn.newEmail();
        invite(owner, UUID.randomUUID(), body(email, "manager", List.of()))
                .statusCode(201).body("status", equalTo("pending")).body("role", equalTo("manager"));

        String token = signIn(email);
        accept(token, invitationSender.lastTokenSentTo(email))
                .statusCode(201).body("id", equalTo(owner.organizationId().toString())).body("role", equalTo("manager"));

        given().spec(owner.spec()).get("/api/v1/members")
                .then().statusCode(200).body("", hasSize(2)).body("status", hasItem("active"));
    }

    @Test
    void retryWithTheSameKeySendsOneEmail() {
        var owner = TestTenants.owner();
        var email = SignIn.newEmail();
        var key = UUID.randomUUID();
        String id = invite(owner, key, body(email, "manager", List.of())).statusCode(201).extract().path("id");

        invite(owner, key, body(email, "manager", List.of())).statusCode(201).body("id", equalTo(id));

        assertEquals(1, invitationSender.tokensSentTo(email).size());
    }

    @Test
    void emailThatIsAlreadyAMemberIsRefused() {
        var owner = TestTenants.owner();

        invite(owner, UUID.randomUUID(), body(owner.email(), "manager", List.of()))
                .statusCode(409).body("code", equalTo("MEMBER_CONFLICT"));
    }

    @Test
    void ownersAreNotInvited() {
        invite(TestTenants.owner(), UUID.randomUUID(), body(SignIn.newEmail(), "owner", List.of()))
                .statusCode(422).body("code", equalTo("OWNER_CHANGE_NOT_ALLOWED"));
    }

    @Test
    void staffNeedAnEstablishment() {
        invite(TestTenants.owner(), UUID.randomUUID(), body(SignIn.newEmail(), "staff", List.of()))
                .statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void staffCannotWorkAtAnotherOrganizationsEstablishment() {
        var foreign = TestTenants.firstEstablishmentOf(TestTenants.owner());

        invite(TestTenants.owner(), UUID.randomUUID(), body(SignIn.newEmail(), "staff", List.of(foreign)))
                .statusCode(404).body("code", equalTo("ESTABLISHMENT_NOT_FOUND"));
    }

    @Test
    void staffCannotInvite() {
        var staff = TestTenants.staffOf(TestTenants.owner());

        invite(staff, UUID.randomUUID(), body(SignIn.newEmail(), "manager", List.of()))
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void managerInvitesStaff() {
        var owner = TestTenants.owner();
        var manager = TestTenants.managerOf(owner);

        invite(manager, UUID.randomUUID(), body(SignIn.newEmail(), "staff", List.of(TestTenants.firstEstablishmentOf(owner))))
                .statusCode(201).body("role", equalTo("staff"));
    }

    @Test
    void teamCanBeManagedWhileThePlanIsInactive() {
        var owner = TestTenants.owner();
        clock.advance(Duration.ofDays(15));

        invite(owner, UUID.randomUUID(), body(SignIn.newEmail(), "manager", List.of())).statusCode(201);
    }

    @Test
    void invitationIsAcceptedOnlyWithTheInvitedEmail() {
        var owner = TestTenants.owner();
        var email = SignIn.newEmail();
        invite(owner, UUID.randomUUID(), body(email, "manager", List.of())).statusCode(201);

        accept(TestTenants.outsider(), invitationSender.lastTokenSentTo(email))
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void retriedAcceptanceReturnsTheSameMembership() {
        var owner = TestTenants.owner();
        var email = SignIn.newEmail();
        invite(owner, UUID.randomUUID(), body(email, "manager", List.of())).statusCode(201);
        var token = signIn(email);
        var invitation = invitationSender.lastTokenSentTo(email);
        String memberId = accept(token, invitation).statusCode(201).extract().path("memberId");

        accept(token, invitation).statusCode(201).body("memberId", equalTo(memberId));
    }

    @Test
    void invitationExpiresAfterSevenDays() {
        var owner = TestTenants.owner();
        var email = SignIn.newEmail();
        invite(owner, UUID.randomUUID(), body(email, "manager", List.of())).statusCode(201);
        var token = signIn(email);
        clock.advance(Duration.ofDays(7));

        accept(token, invitationSender.lastTokenSentTo(email))
                .statusCode(422).body("code", equalTo("INVITATION_EXPIRED"));
    }

    @Test
    void unknownTokenIsNotFound() {
        accept(TestTenants.outsider(), "no-such-token").statusCode(404).body("code", equalTo("INVITATION_NOT_FOUND"));
    }

    @Test
    void renewedInvitationReplacesTheOldLink() {
        var owner = TestTenants.owner();
        var email = SignIn.newEmail();
        String memberId = invite(owner, UUID.randomUUID(), body(email, "manager", List.of())).extract().path("id");
        var oldToken = invitationSender.lastTokenSentTo(email);
        var key = UUID.randomUUID();

        renew(owner, key, memberId).statusCode(200);
        renew(owner, key, memberId).statusCode(200);

        assertEquals(2, invitationSender.tokensSentTo(email).size());
        var token = signIn(email);
        accept(token, oldToken).statusCode(404);
        accept(token, invitationSender.lastTokenSentTo(email)).statusCode(201);
    }

    @Test
    void acceptedInvitationIsNotRenewed() {
        var owner = TestTenants.owner();
        var manager = TestTenants.managerOf(owner);

        renew(owner, UUID.randomUUID(), manager.memberId().toString())
                .statusCode(422).body("code", equalTo("INVITATION_NOT_PENDING"));
    }

    private String signIn(String email) {
        return SignIn.as(email, emailSender).extract().path("accessToken");
    }

    static ValidatableResponse invite(TestTenant tenant, UUID key, Map<String, Object> body) {
        return given().spec(tenant.spec()).header("Idempotency-Key", key.toString()).contentType(ContentType.JSON)
                .body(body).post("/api/v1/members").then();
    }

    private static ValidatableResponse accept(String accessToken, String invitationToken) {
        return given().auth().oauth2(accessToken).contentType(ContentType.JSON).body(Map.of("token", invitationToken))
                .post("/api/v1/invitation-acceptances").then();
    }

    private static ValidatableResponse renew(TestTenant tenant, UUID key, String memberId) {
        return given().spec(tenant.spec()).header("Idempotency-Key", key.toString())
                .post("/api/v1/members/" + memberId + "/invitation-renewals").then();
    }

    static Map<String, Object> body(String email, String role, List<UUID> establishmentIds) {
        var body = new HashMap<String, Object>();
        body.put("email", email);
        body.put("displayName", "Lucía");
        body.put("role", role);
        body.put("establishmentIds", establishmentIds);
        return body;
    }
}
