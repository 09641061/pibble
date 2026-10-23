package com.takodu.api.organization.interfaces.rest;

import com.takodu.api.iam.interfaces.rest.SignIn;
import com.takodu.api.shared.testing.TestTenant;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class OwnershipTransfersControllerTest {

    @Test
    void ownerHandsTheOrganizationToAManager() {
        var owner = TestTenants.owner();
        var manager = TestTenants.managerOf(owner);

        transfer(owner, UUID.randomUUID(), manager.memberId())
                .statusCode(201).body("id", equalTo(manager.memberId().toString())).body("role", equalTo("owner"));

        given().auth().oauth2(owner.accessToken()).get("/api/v1/organizations").then().body("[0].role", equalTo("manager"));
        renameAs(owner).statusCode(403);
        renameAs(manager).statusCode(200);
    }

    @Test
    void retryReturnsTheNewOwner() {
        var owner = TestTenants.owner();
        var manager = TestTenants.managerOf(owner);
        var key = UUID.randomUUID();
        transfer(owner, key, manager.memberId()).statusCode(201);

        transfer(owner, key, manager.memberId()).statusCode(201).body("id", equalTo(manager.memberId().toString()));
    }

    @Test
    void pendingInvitationCannotOwn() {
        var owner = TestTenants.owner();
        TestTenants.invite(owner, SignIn.newEmail(), "manager", List.of());
        String pending = given().spec(owner.spec()).get("/api/v1/members")
                .then().extract().path("find { it.status == 'pending' }.id");

        transfer(owner, UUID.randomUUID(), UUID.fromString(pending))
                .statusCode(422).body("code", equalTo("MEMBER_NOT_ACTIVE"));
    }

    @Test
    void managerCannotTransfer() {
        var owner = TestTenants.owner();
        var manager = TestTenants.managerOf(owner);

        transfer(manager, UUID.randomUUID(), manager.memberId()).statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void twoSimultaneousTransfersLeaveOneOwner() {
        var owner = TestTenants.owner();
        var first = TestTenants.managerOf(owner);
        var second = TestTenants.managerOf(owner);

        var a = CompletableFuture.supplyAsync(() -> transfer(owner, UUID.randomUUID(), first.memberId()).extract().statusCode());
        var b = CompletableFuture.supplyAsync(() -> transfer(owner, UUID.randomUUID(), second.memberId()).extract().statusCode());

        assertEquals(List.of(201, 403), List.of(a.join(), b.join()).stream().sorted().toList());
        List<String> owners = given().spec(first.spec()).get("/api/v1/members")
                .then().extract().path("findAll { it.role == 'owner' }.id");
        assertEquals(1, owners.size());
    }

    private static ValidatableResponse transfer(TestTenant tenant, UUID key, UUID newOwnerId) {
        return given().spec(tenant.spec()).header("Idempotency-Key", key.toString()).contentType(ContentType.JSON)
                .body(Map.of("newOwnerId", newOwnerId.toString())).post("/api/v1/ownership-transfers").then();
    }

    private static ValidatableResponse renameAs(TestTenant tenant) {
        return given().spec(tenant.spec()).contentType(ContentType.JSON).body(Map.of("name", "Nuevo nombre"))
                .put("/api/v1/organization").then();
    }
}
