package com.takodu.api.organization.interfaces.rest;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class EstablishmentOpeningHoursControllerTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void newEstablishmentIsClosed() {
        var owner = TestTenants.owner();

        given().spec(owner.spec()).get(path(TestTenants.firstEstablishmentOf(owner)))
                .then().statusCode(200).body("ranges", hasSize(0));
    }

    @Test
    void ownerSetsASplitShift() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);

        replace(owner, establishment, List.of(range(1, "15:00", "20:00"), range(1, "09:00", "13:00"), range(6, "09:00", "14:00")))
                .statusCode(200).body("ranges", hasSize(3));

        given().spec(owner.spec()).get(path(establishment))
                .then().statusCode(200)
                .body("ranges[0].weekday", equalTo(1))
                .body("ranges[0].opensAt", equalTo("09:00:00"))
                .body("ranges[2].weekday", equalTo(6));
    }

    @Test
    void replacingTwiceKeepsOnlyTheLastWeek() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        replace(owner, establishment, List.of(range(1, "09:00", "13:00"), range(2, "09:00", "13:00"))).statusCode(200);

        replace(owner, establishment, List.of(range(3, "10:00", "12:00"))).statusCode(200);

        given().spec(owner.spec()).get(path(establishment))
                .then().body("ranges", hasSize(1)).body("ranges[0].weekday", equalTo(3));
    }

    @Test
    void managerSetsHoursButStaffCannot() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);

        replace(TestTenants.managerOf(owner), establishment, List.of(range(2, "10:00", "19:00"))).statusCode(200);
        replace(TestTenants.staffOf(owner), establishment, List.of())
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void overlappingRangesAreRefused() {
        var owner = TestTenants.owner();

        replace(owner, TestTenants.firstEstablishmentOf(owner), List.of(range(5, "09:00", "14:00"), range(5, "13:00", "18:00")))
                .statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void rangePastMidnightIsRefused() {
        var owner = TestTenants.owner();

        replace(owner, TestTenants.firstEstablishmentOf(owner), List.of(range(6, "20:00", "02:00")))
                .statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void weekdayOutsideTheWeekIsRefused() {
        var owner = TestTenants.owner();

        replace(owner, TestTenants.firstEstablishmentOf(owner), List.of(range(8, "09:00", "12:00"))).statusCode(400);
    }

    @Test
    void changingHoursNeedsAnActivePlan() {
        var owner = TestTenants.owner();
        var establishment = TestTenants.firstEstablishmentOf(owner);
        clock.advance(Duration.ofDays(15));

        replace(owner, establishment, List.of(range(1, "09:00", "12:00"))).statusCode(402);
    }

    @Test
    void establishmentOfAnotherOrganizationIsNotFound() {
        var other = TestTenants.firstEstablishmentOf(TestTenants.owner());

        replace(TestTenants.owner(), other, List.of()).statusCode(404);
    }

    static String path(UUID establishmentId) {
        return "/api/v1/establishments/" + establishmentId + "/opening-hours";
    }

    static ValidatableResponse replace(TestTenant tenant, UUID establishmentId, List<Map<String, Object>> ranges) {
        return given().spec(tenant.spec()).contentType(ContentType.JSON).body(Map.of("ranges", ranges))
                .put(path(establishmentId)).then();
    }

    static Map<String, Object> range(int weekday, String opensAt, String closesAt) {
        return Map.of("weekday", weekday, "opensAt", opensAt, "closesAt", closesAt);
    }
}
