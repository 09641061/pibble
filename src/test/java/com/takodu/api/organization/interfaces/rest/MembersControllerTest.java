package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.booking.interfaces.rest.AppointmentsControllerTest;
import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import com.takodu.api.iam.interfaces.rest.SignIn;
import com.takodu.api.organization.application.internal.outboundservices.email.RecordingInvitationSender;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
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
    void anyMemberRenamesOnlyThemselves() {
        var owner = TestTenants.owner();
        var staff = TestTenants.staffOf(owner);
        String ownerName = given().spec(owner.spec()).get("/api/v1/members").then().statusCode(200)
                .extract().path("find { it.id == '%s' }.displayName".formatted(owner.memberId()));

        given().spec(staff.spec()).contentType(ContentType.JSON).body(Map.of("displayName", "  Carla Ríos "))
                .put("/api/v1/members/me").then().statusCode(200)
                .body("id", equalTo(staff.memberId().toString()))
                .body("displayName", equalTo("Carla Ríos"));

        given().spec(owner.spec()).get("/api/v1/members").then().statusCode(200)
                .body("find { it.id == '%s' }.displayName".formatted(staff.memberId()), equalTo("Carla Ríos"))
                .body("find { it.id == '%s' }.displayName".formatted(owner.memberId()), equalTo(ownerName));
        given().spec(staff.spec()).contentType(ContentType.JSON).body(Map.of("displayName", " "))
                .put("/api/v1/members/me").then().statusCode(400);
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

    @Test
    void managerBecomesStaffOfOneEstablishment() {
        var owner = TestTenants.owner();
        var manager = TestTenants.managerOf(owner);
        var establishment = TestTenants.firstEstablishmentOf(owner);

        update(owner, manager.memberId(), "Lucía", false, "staff", List.of(establishment))
                .statusCode(200)
                .body("role", equalTo("staff"))
                .body("bookable", equalTo(false))
                .body("establishmentIds", hasItem(establishment.toString()));
    }

    @Test
    void ownerEditsTheirNameButKeepsTheRole() {
        var owner = TestTenants.owner();

        update(owner, owner.memberId(), "Don Pepe", true, "owner", List.of()).statusCode(200).body("displayName", equalTo("Don Pepe"));
        update(owner, owner.memberId(), "Don Pepe", true, "manager", List.of())
                .statusCode(422).body("code", equalTo("OWNER_CHANGE_NOT_ALLOWED"));
    }

    @Test
    void nobodyBecomesOwnerByUpdate() {
        var owner = TestTenants.owner();
        var manager = TestTenants.managerOf(owner);

        update(owner, manager.memberId(), "Lucía", true, "owner", List.of())
                .statusCode(422).body("code", equalTo("OWNER_CHANGE_NOT_ALLOWED"));
    }

    @Test
    void removedMemberLosesAccess() {
        var owner = TestTenants.owner();
        var staff = TestTenants.staffOf(owner);

        remove(owner, staff.memberId()).statusCode(204);

        given().spec(staff.spec()).get("/api/v1/organization").then().statusCode(403);
        given().spec(owner.spec()).get("/api/v1/members").then().body("", hasSize(1));
    }

    @Test
    void revokedInvitationCannotBeAccepted() {
        var owner = TestTenants.owner();
        var email = SignIn.newEmail();
        String memberId = invite(owner, UUID.randomUUID(), body(email, "manager", List.of())).extract().path("id");

        remove(owner, UUID.fromString(memberId)).statusCode(204);

        accept(signIn(email), invitationSender.lastTokenSentTo(email)).statusCode(404);
    }

    @Test
    void removedEmailCanBeInvitedAgain() {
        var owner = TestTenants.owner();
        var staff = TestTenants.staffOf(owner);
        remove(owner, staff.memberId()).statusCode(204);

        invite(owner, UUID.randomUUID(), body(staff.email(), "manager", List.of())).statusCode(201);
    }

    @Test
    void ownerIsNotRemoved() {
        var owner = TestTenants.owner();

        remove(TestTenants.managerOf(owner), owner.memberId())
                .statusCode(422).body("code", equalTo("OWNER_CHANGE_NOT_ALLOWED"));
    }

    @Test
    void memberWithUpcomingAppointmentsIsNotRemoved() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        salon.performedBy(staff);
        String appointment = AppointmentsControllerTest.book(salon.owner(), UUID.randomUUID(),
                AppointmentsControllerTest.body(salon, staff.memberId(), TestSalon.at(clock, 1, "10:00")))
                .statusCode(201).extract().path("id");

        remove(salon.owner(), staff.memberId()).statusCode(422).body("code", equalTo("MEMBER_HAS_UPCOMING_APPOINTMENTS"));

        given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json").body(Map.of("reason", "Deja el equipo"))
                .post("/api/v1/appointments/" + appointment + "/cancellations").then().statusCode(200);
        remove(salon.owner(), staff.memberId()).statusCode(204);
    }

    @Test
    void pastAppointmentsDoNotKeepAMember() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        salon.performedBy(staff);
        AppointmentsControllerTest.book(salon.owner(), UUID.randomUUID(),
                AppointmentsControllerTest.body(salon, staff.memberId(), TestSalon.at(clock, 1, "10:00"))).statusCode(201);
        clock.advance(Duration.ofDays(2));

        remove(salon.owner(), staff.memberId()).statusCode(204);
    }

    @Test
    void staffCannotRemoveMembers() {
        var owner = TestTenants.owner();
        var manager = TestTenants.managerOf(owner);

        remove(TestTenants.staffOf(owner), manager.memberId()).statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void memberOfAnotherOrganizationIsNotFound() {
        var foreign = TestTenants.managerOf(TestTenants.owner());

        remove(TestTenants.owner(), foreign.memberId()).statusCode(404).body("code", equalTo("MEMBER_NOT_FOUND"));
    }

    private static ValidatableResponse update(TestTenant tenant, UUID memberId, String displayName, boolean bookable,
                                              String role, List<UUID> establishmentIds) {
        return given().spec(tenant.spec()).contentType(ContentType.JSON)
                .body(Map.of("displayName", displayName, "bookable", bookable, "role", role,
                        "establishmentIds", establishmentIds))
                .put("/api/v1/members/" + memberId).then();
    }

    private static ValidatableResponse remove(TestTenant tenant, UUID memberId) {
        return given().spec(tenant.spec()).delete("/api/v1/members/" + memberId).then();
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
