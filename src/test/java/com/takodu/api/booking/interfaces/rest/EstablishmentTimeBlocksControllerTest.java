package com.takodu.api.booking.interfaces.rest;

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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class EstablishmentTimeBlocksControllerTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void staffBlockTheirOwnTime() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();

        create(staff, salon.establishmentId(), UUID.randomUUID(), body(staff.memberId(), 1, "Cita médica"))
                .statusCode(201)
                .body("staffMemberId", equalTo(staff.memberId().toString()))
                .body("reason", equalTo("Cita médica"));

        list(staff, salon.establishmentId()).statusCode(200).body("", hasSize(1));
    }

    @Test
    void managerBlocksAStaffMemberAndTheWholeEstablishment() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        var manager = TestTenants.managerOf(salon.owner());

        create(manager, salon.establishmentId(), UUID.randomUUID(), body(staff.memberId(), 1, null)).statusCode(201);
        create(manager, salon.establishmentId(), UUID.randomUUID(), body(null, 2, "Feriado"))
                .statusCode(201).body("staffMemberId", nullValue());

        list(staff, salon.establishmentId()).body("", hasSize(2));
    }

    @Test
    void staffCannotBlockSomeoneElseOrEveryone() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();

        create(staff, salon.establishmentId(), UUID.randomUUID(), body(salon.owner().memberId(), 1, null))
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
        create(staff, salon.establishmentId(), UUID.randomUUID(), body(null, 1, null))
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void staffOfAnotherEstablishmentCannotBlockOrSeeBlocksThere() {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();
        var staff = salon.hireStaff();

        create(staff, surco, UUID.randomUUID(), body(staff.memberId(), 1, null))
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
        list(staff, surco).statusCode(403);
    }

    @Test
    void memberMustWorkAtTheEstablishment() {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();
        var staff = salon.hireStaff();

        create(salon.owner(), surco, UUID.randomUUID(), body(staff.memberId(), 1, null))
                .statusCode(422).body("code", equalTo("MEMBER_NOT_AT_ESTABLISHMENT"));
    }

    @Test
    void retryWithTheSameKeyReturnsTheSameBlock() {
        var salon = TestSalon.open();
        var key = UUID.randomUUID();
        String id = create(salon.owner(), salon.establishmentId(), key, body(null, 1, null))
                .statusCode(201).extract().path("id");

        create(salon.owner(), salon.establishmentId(), key, body(null, 1, null)).statusCode(201).body("id", equalTo(id));
        list(salon.owner(), salon.establishmentId()).body("", hasSize(1));
    }

    @Test
    void blocksCanChangeWhileThePlanIsInactive() {
        var salon = TestSalon.open();
        clock.advance(Duration.ofDays(15));

        create(salon.owner(), salon.establishmentId(), UUID.randomUUID(), body(null, 1, null)).statusCode(201);
    }

    @Test
    void endMustComeAfterStart() {
        var salon = TestSalon.open();
        var body = body(null, 1, null);
        body.put("end", body.get("start"));

        create(salon.owner(), salon.establishmentId(), UUID.randomUUID(), body).statusCode(400);
    }

    @Test
    void listingNeedsInstantsAtMost42DaysApart() {
        var salon = TestSalon.open();
        var from = Instant.now();

        given().spec(salon.owner().spec()).queryParam("from", from.toString())
                .queryParam("to", from.plus(Duration.ofDays(43)).toString())
                .get(path(salon.establishmentId())).then().statusCode(400);
        given().spec(salon.owner().spec()).queryParam("from", "yesterday").queryParam("to", from.toString())
                .get(path(salon.establishmentId())).then().statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void staffDeleteOnlyTheirOwnBlocks() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        String own = create(staff, salon.establishmentId(), UUID.randomUUID(), body(staff.memberId(), 1, null))
                .extract().path("id");
        String everyone = create(salon.owner(), salon.establishmentId(), UUID.randomUUID(), body(null, 1, null))
                .extract().path("id");

        delete(staff, everyone).statusCode(403).body("code", equalTo("ACCESS_DENIED"));
        delete(staff, own).statusCode(204);
        delete(staff, own).statusCode(404).body("code", equalTo("TIME_BLOCK_NOT_FOUND"));
        delete(salon.owner(), everyone).statusCode(204);
    }

    @Test
    void idsOfAnotherOrganizationAreNotFound() {
        var salon = TestSalon.open();
        var other = TestSalon.open();
        String block = create(other.owner(), other.establishmentId(), UUID.randomUUID(), body(null, 1, null))
                .extract().path("id");

        create(salon.owner(), other.establishmentId(), UUID.randomUUID(), body(null, 1, null)).statusCode(404);
        create(salon.owner(), salon.establishmentId(), UUID.randomUUID(), body(other.owner().memberId(), 1, null))
                .statusCode(404).body("code", equalTo("MEMBER_NOT_FOUND"));
        list(salon.owner(), other.establishmentId()).statusCode(404);
        delete(salon.owner(), block).statusCode(404);
    }

    static String path(UUID establishmentId) {
        return "/api/v1/establishments/" + establishmentId + "/time-blocks";
    }

    static ValidatableResponse create(TestTenant tenant, UUID establishmentId, UUID key, Map<String, Object> body) {
        return given().spec(tenant.spec()).header("Idempotency-Key", key.toString()).contentType(ContentType.JSON)
                .body(body).post(path(establishmentId)).then();
    }

    static ValidatableResponse list(TestTenant tenant, UUID establishmentId) {
        var now = Instant.now();
        return given().spec(tenant.spec()).queryParam("from", now.toString())
                .queryParam("to", now.plus(Duration.ofDays(7)).toString())
                .get(path(establishmentId)).then();
    }

    static ValidatableResponse delete(TestTenant tenant, String timeBlockId) {
        return given().spec(tenant.spec()).delete("/api/v1/time-blocks/" + timeBlockId).then();
    }

    /** A two-hour block starting at 10:00 Lima, {@code daysAhead} days from today. */
    Map<String, Object> body(UUID staffMemberId, int daysAhead, String reason) {
        var start = TestSalon.at(clock, daysAhead, "10:00");
        var body = new HashMap<String, Object>();
        body.put("staffMemberId", staffMemberId);
        body.put("start", start.toString());
        body.put("end", start.plus(Duration.ofHours(2)).toString());
        body.put("reason", reason);
        return body;
    }
}
