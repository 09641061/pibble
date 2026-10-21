package com.takodu.api.booking.interfaces.rest;

import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.UnaryOperator;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
class ServiceSlotsControllerTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void wholeOpenDayHasASlotEvery15Minutes() {
        var salon = TestSalon.open();

        slots(salon.owner(), salon.serviceId(), request -> request.queryParam("date", TestSalon.day(clock, 1).toString()))
                .statusCode(200)
                .body("", hasSize(33))
                .body("[0].start", equalTo(TestSalon.at(clock, 1, "09:00").toString()))
                .body("[0].end", equalTo(TestSalon.at(clock, 1, "10:00").toString()))
                .body("[0].freeMemberIds", contains(salon.owner().memberId().toString()))
                .body("[32].start", equalTo(TestSalon.at(clock, 1, "17:00").toString()));
    }

    @Test
    void bookedTimeLeavesTheSlots() {
        var salon = TestSalon.open();
        AppointmentsControllerTest.book(salon.owner(), UUID.randomUUID(),
                AppointmentsControllerTest.body(salon, null, TestSalon.at(clock, 1, "10:00"))).statusCode(201);

        slots(salon.owner(), salon.serviceId(), request -> request.queryParam("date", TestSalon.day(clock, 1).toString()))
                .body("", hasSize(26))
                .body("start", not(hasItem(TestSalon.at(clock, 1, "10:30").toString())))
                .body("start", hasItem(TestSalon.at(clock, 1, "11:00").toString()));
    }

    @Test
    void customersOtherAppointmentLeavesTheSlotsOfEveryMember() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        salon.performedBy(salon.owner(), staff);
        AppointmentsControllerTest.book(salon.owner(), UUID.randomUUID(),
                AppointmentsControllerTest.body(salon, staff.memberId(), TestSalon.at(clock, 1, "10:00"))).statusCode(201);

        slots(salon.owner(), salon.serviceId(), request -> request.queryParam("date", TestSalon.day(clock, 1).toString())
                .queryParam("customerId", salon.customerId().toString()))
                .body("start", not(hasItem(TestSalon.at(clock, 1, "10:30").toString())));
        slots(salon.owner(), salon.serviceId(), request -> request.queryParam("date", TestSalon.day(clock, 1).toString()))
                .body("find { it.start == '%s' }.freeMemberIds".formatted(TestSalon.at(clock, 1, "10:30")),
                        contains(salon.owner().memberId().toString()));
    }

    @Test
    void oneMembersSlots() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        salon.performedBy(salon.owner(), staff);

        slots(salon.owner(), salon.serviceId(), request -> request.queryParam("date", TestSalon.day(clock, 1).toString())
                .queryParam("staffMemberId", staff.memberId().toString()))
                .body("[0].freeMemberIds", contains(staff.memberId().toString()));
    }

    @Test
    void dateIsRequired() {
        var salon = TestSalon.open();

        slots(salon.owner(), salon.serviceId(), UnaryOperator.identity()).statusCode(400);
        slots(salon.owner(), salon.serviceId(), request -> request.queryParam("date", "2026-13-01")).statusCode(400);
    }

    @Test
    void staffSeeSlotsOnlyInTheirEstablishments() {
        var salon = TestSalon.open();
        var staff = TestTenants.staffOf(salon.owner(), salon.openSecondEstablishment());

        slots(staff, salon.serviceId(), request -> request.queryParam("date", TestSalon.day(clock, 1).toString()))
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void idsOfAnotherOrganizationAreNotFound() {
        var salon = TestSalon.open();
        var other = TestSalon.open();

        slots(salon.owner(), other.serviceId(), request -> request.queryParam("date", TestSalon.day(clock, 1).toString()))
                .statusCode(404);
        slots(salon.owner(), salon.serviceId(), request -> request.queryParam("date", TestSalon.day(clock, 1).toString())
                .queryParam("customerId", other.customerId().toString()))
                .statusCode(404).body("code", equalTo("CUSTOMER_NOT_FOUND"));
    }

    static ValidatableResponse slots(TestTenant tenant, UUID serviceId, UnaryOperator<RequestSpecification> query) {
        return query.apply(given().spec(tenant.spec())).get("/api/v1/services/" + serviceId + "/slots").then();
    }
}
