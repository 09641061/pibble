package com.takodu.api.shared.testing;

import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import com.takodu.api.iam.interfaces.rest.SignIn;
import com.takodu.api.organization.application.internal.outboundservices.email.RecordingInvitationSender;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Creates users, organizations and memberships through the endpoints, so endpoint tests never build tokens by
 * hand (takodu-tenancy). Call it only from a {@code @QuarkusTest}.
 */
public final class TestTenants {
    private TestTenants() { }

    /** @return the owner of a new organization on a trial plan */
    public static TestTenant owner() {
        var email = SignIn.newEmail();
        var token = signIn(email);
        String organizationId = given().auth().oauth2(token).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Test organization", "establishmentName", "Main branch", "ownerDisplayName", "Owner"))
                .post("/api/v1/organizations")
                .then().statusCode(201).extract().path("id");
        return new TestTenant(token, email, UUID.fromString(organizationId), membershipIn(token, organizationId));
    }

    /** @return a manager who accepted an invitation to the tenant's organization */
    public static TestTenant managerOf(TestTenant tenant) {
        return join(tenant, "manager", List.of());
    }

    /** @return a staff member of the tenant's organization assigned to its first establishment */
    public static TestTenant staffOf(TestTenant tenant) {
        return join(tenant, "staff", List.of(firstEstablishmentOf(tenant)));
    }

    /** @return a staff member of the tenant's organization assigned to the given establishments */
    public static TestTenant staffOf(TestTenant tenant, UUID... establishmentIds) {
        return join(tenant, "staff", Arrays.asList(establishmentIds));
    }

    /** Invites the email as {@code tenant} and returns the token the invitation email carried. */
    public static String invite(TestTenant tenant, String email, String role, List<UUID> establishmentIds) {
        given().spec(tenant.spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "displayName", role + " " + email.substring(5, 13), "role", role,
                        "establishmentIds", establishmentIds))
                .post("/api/v1/members")
                .then().statusCode(201);
        return Arc.container().instance(RecordingInvitationSender.class).get().lastTokenSentTo(email);
    }

    private static TestTenant join(TestTenant tenant, String role, List<UUID> establishmentIds) {
        var email = SignIn.newEmail();
        var invitationToken = invite(tenant, email, role, establishmentIds);
        var token = signIn(email);
        String memberId = given().auth().oauth2(token).contentType(ContentType.JSON)
                .body(Map.of("token", invitationToken))
                .post("/api/v1/invitation-acceptances")
                .then().statusCode(201).extract().path("memberId");
        return new TestTenant(token, email, tenant.organizationId(), UUID.fromString(memberId));
    }

    /** @return the first active establishment the member sees */
    public static UUID firstEstablishmentOf(TestTenant tenant) {
        String id = given().spec(tenant.spec()).get("/api/v1/establishments")
                .then().statusCode(200).extract().path("[0].id");
        return UUID.fromString(id);
    }

    /** Does what the Takodu team does after a payment for the max plan (O4, O5). */
    public static void upgradeToMax(TestTenant tenant) {
        QuarkusTransaction.requiringNew().run(() -> Arc.container().instance(EntityManager.class).get()
                .createNativeQuery("update organizations set plan = 'max' where id = :id")
                .setParameter("id", tenant.organizationId())
                .executeUpdate());
    }

    /** @return a user who signed in but belongs to no organization */
    public static String outsider() {
        return signIn(SignIn.newEmail());
    }

    static String signIn(String email) {
        return SignIn.as(email, Arc.container().instance(RecordingEmailSender.class).get()).extract().path("accessToken");
    }

    static UUID membershipIn(String token, String organizationId) {
        String memberId = given().auth().oauth2(token).get("/api/v1/organizations")
                .then().statusCode(200)
                .extract().path("find { it.id == '%s' }.memberId".formatted(organizationId));
        return UUID.fromString(memberId);
    }
}
