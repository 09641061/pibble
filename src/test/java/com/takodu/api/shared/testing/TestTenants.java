package com.takodu.api.shared.testing;

import com.takodu.api.iam.application.internal.outboundservices.email.RecordingEmailSender;
import com.takodu.api.iam.interfaces.rest.SignIn;
import io.quarkus.arc.Arc;
import io.restassured.http.ContentType;

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
