package com.takodu.api.booking.interfaces.rest;

import com.takodu.api.booking.RecordingBookingEvents;
import com.takodu.api.booking.interfaces.events.AppointmentBookedIntegrationEvent;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@QuarkusTest
public class AppointmentsControllerTest {

    @Inject
    TestClock clock;

    @Inject
    RecordingBookingEvents events;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void staffBookTheChosenMemberAtTheServicesPrice() {
        var salon = TestSalon.open();
        var start = TestSalon.at(clock, 1, "10:00");

        String id = book(salon.owner(), UUID.randomUUID(), body(salon, salon.owner().memberId(), start))
                .statusCode(201)
                .body("establishmentId", equalTo(salon.establishmentId().toString()))
                .body("staffMemberId", equalTo(salon.owner().memberId().toString()))
                .body("start", equalTo(start.toString()))
                .body("end", equalTo(start.plus(Duration.ofHours(1)).toString()))
                .body("priceCents", equalTo(3500))
                .body("currency", equalTo("PEN"))
                .body("status", equalTo("confirmed"))
                .body("source", equalTo("staff"))
                .extract().path("id");

        get(salon.owner(), id).statusCode(200).body("id", equalTo(id));
        calendar(salon.owner(), salon.establishmentId()).statusCode(200).body("", hasSize(1));
    }

    @Test
    void bookingPublishesOneEventEvenWhenRetried() {
        var salon = TestSalon.open();
        var key = UUID.randomUUID();
        var body = body(salon, null, TestSalon.at(clock, 1, "10:00"));
        String id = book(salon.owner(), key, body).statusCode(201).extract().path("id");

        book(salon.owner(), key, body).statusCode(201).body("id", equalTo(id));

        var published = events.about(UUID.fromString(id));
        assertEquals(1, published.size());
        var booked = assertInstanceOf(AppointmentBookedIntegrationEvent.class, published.getFirst());
        assertEquals(salon.owner().organizationId(), booked.organizationId());
        assertEquals("staff", booked.source());
        calendar(salon.owner(), salon.establishmentId()).body("", hasSize(1));
    }

    @Test
    void laterServicePriceDoesNotChangeTheAppointment() {
        var salon = TestSalon.open();
        String id = book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 1, "10:00")))
                .extract().path("id");

        given().spec(salon.owner().spec()).contentType(ContentType.JSON)
                .body(Map.of("name", "Corte", "durationMinutes", 90, "priceCents", 5000,
                        "performerIds", List.of(salon.owner().memberId())))
                .put("/api/v1/services/" + salon.serviceId()).then().statusCode(200);

        get(salon.owner(), id).body("priceCents", equalTo(3500)).body("end", equalTo(
                TestSalon.at(clock, 1, "11:00").toString()));
    }

    @Test
    void withoutAChoiceTheLeastBusyMemberThenTheFirstToJoinTakesIt() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        salon.performedBy(staff, salon.owner());

        book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 1, "10:00")))
                .statusCode(201).body("staffMemberId", equalTo(salon.owner().memberId().toString()));
        book(salon.owner(), UUID.randomUUID(), body(salon.newCustomer(), salon, null, TestSalon.at(clock, 1, "15:00")))
                .statusCode(201).body("staffMemberId", equalTo(staff.memberId().toString()));
    }

    @Test
    void backToBackAppointmentsFit() {
        var salon = TestSalon.open();

        book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 1, "10:00"))).statusCode(201);
        book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 1, "11:00"))).statusCode(201);
    }

    @Test
    void takenMemberOrBusyCustomerIsAConflict() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        salon.performedBy(salon.owner(), staff);
        var ten = TestSalon.at(clock, 1, "10:00");
        book(salon.owner(), UUID.randomUUID(), body(salon, salon.owner().memberId(), ten)).statusCode(201);

        book(salon.owner(), UUID.randomUUID(), body(salon.newCustomer(), salon, salon.owner().memberId(),
                ten.plus(Duration.ofMinutes(30)))).statusCode(409).body("code", equalTo("SLOT_UNAVAILABLE"));
        book(salon.owner(), UUID.randomUUID(), body(salon, staff.memberId(), ten))
                .statusCode(409).body("code", equalTo("SLOT_UNAVAILABLE"));
    }

    @Test
    void simultaneousBookingsOfOneSlotEndWithOneAppointment() {
        var salon = TestSalon.open();
        var ten = TestSalon.at(clock, 1, "10:00");
        var bodies = IntStream.range(0, 6).mapToObj(i -> body(salon.newCustomer(), salon, salon.owner().memberId(), ten))
                .toList();

        var statuses = bodies.stream()
                .map(body -> CompletableFuture.supplyAsync(() ->
                        book(salon.owner(), UUID.randomUUID(), body).extract().statusCode()))
                .toList().stream().map(CompletableFuture::join).sorted().toList();

        assertEquals(List.of(201, 409, 409, 409, 409, 409), statuses);
        calendar(salon.owner(), salon.establishmentId()).body("", hasSize(1));
    }

    @Test
    void appointmentMustFitTheOpeningHoursAndAvoidBlocks() {
        var salon = TestSalon.open();
        book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 1, "17:30")))
                .statusCode(422).body("code", equalTo("OUTSIDE_OPENING_HOURS"));

        var lunch = TestSalon.at(clock, 1, "13:00");
        EstablishmentTimeBlocksControllerTest.create(salon.owner(), salon.establishmentId(), UUID.randomUUID(),
                Map.of("start", lunch.toString(), "end", lunch.plus(Duration.ofHours(1)).toString())).statusCode(201);

        book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 1, "12:30")))
                .statusCode(422).body("code", equalTo("TIME_BLOCKED"));
    }

    @Test
    void memberWhoDoesNotPerformTheServiceIsRefused() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();

        book(salon.owner(), UUID.randomUUID(), body(salon, staff.memberId(), TestSalon.at(clock, 1, "10:00")))
                .statusCode(422).body("code", equalTo("MEMBER_CANNOT_PERFORM_SERVICE"));
    }

    @Test
    void staffRecordWalkInsOnlyUpTo12HoursBack() {
        var salon = TestSalon.open();
        clock.set(TestSalon.at(clock, 1, "22:30"));

        book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 0, "11:00"))).statusCode(201);
        book(salon.owner(), UUID.randomUUID(), body(salon.newCustomer(), salon, null, TestSalon.at(clock, 0, "09:00")))
                .statusCode(422).body("code", equalTo("OUTSIDE_BOOKING_WINDOW"));
    }

    @Test
    void inactiveServiceIsNotBookable() {
        var salon = TestSalon.open();
        given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .post("/api/v1/services/" + salon.serviceId() + "/deactivations").then().statusCode(200);

        book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 1, "10:00")))
                .statusCode(422).body("code", equalTo("SERVICE_NOT_BOOKABLE"));
    }

    @Test
    void deletedCustomerGetsNoNewAppointments() {
        var salon = TestSalon.open();
        given().spec(salon.owner().spec()).delete("/api/v1/customers/" + salon.customerId()).then().statusCode(204);

        book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 1, "10:00")))
                .statusCode(404).body("code", equalTo("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void bookingNeedsAnActivePlan() {
        var salon = TestSalon.open();
        clock.advance(Duration.ofDays(15));

        book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 1, "10:00")))
                .statusCode(402).body("code", equalTo("PLAN_INACTIVE"));
    }

    @Test
    void staffBookAndSeeOnlyTheirEstablishments() {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();
        var staff = TestTenants.staffOf(salon.owner(), surco);
        var appointment = book(salon.owner(), UUID.randomUUID(), body(salon, null, TestSalon.at(clock, 1, "10:00")))
                .extract().<String>path("id");

        book(staff, UUID.randomUUID(), body(salon.newCustomer(), salon, null, TestSalon.at(clock, 1, "12:00")))
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
        get(staff, appointment).statusCode(403);
        calendar(staff, salon.establishmentId()).statusCode(403);
    }

    @Test
    void idsOfAnotherOrganizationAreNotFound() {
        var salon = TestSalon.open();
        var other = TestSalon.open();
        var ten = TestSalon.at(clock, 1, "10:00");
        String appointment = book(other.owner(), UUID.randomUUID(), body(other, null, ten)).extract().path("id");

        book(salon.owner(), UUID.randomUUID(), body(salon.customerId(), other, null, ten))
                .statusCode(404).body("code", equalTo("SERVICE_NOT_FOUND"));
        book(salon.owner(), UUID.randomUUID(), body(other.customerId(), salon, null, ten))
                .statusCode(404).body("code", equalTo("CUSTOMER_NOT_FOUND"));
        get(salon.owner(), appointment).statusCode(404).body("code", equalTo("APPOINTMENT_NOT_FOUND"));
        calendar(salon.owner(), other.establishmentId()).statusCode(404);
    }

    public static ValidatableResponse book(TestTenant tenant, UUID key, Map<String, Object> body) {
        return given().spec(tenant.spec()).header("Idempotency-Key", key.toString()).contentType(ContentType.JSON)
                .body(body).post("/api/v1/appointments").then();
    }

    public static ValidatableResponse get(TestTenant tenant, String appointmentId) {
        return given().spec(tenant.spec()).get("/api/v1/appointments/" + appointmentId).then();
    }

    static ValidatableResponse calendar(TestTenant tenant, UUID establishmentId) {
        var now = Instant.now();
        return given().spec(tenant.spec()).queryParam("from", now.minus(Duration.ofDays(1)).toString())
                .queryParam("to", now.plus(Duration.ofDays(7)).toString())
                .get("/api/v1/establishments/" + establishmentId + "/appointments").then();
    }

    /** The salon's service for its first customer. */
    public static Map<String, Object> body(TestSalon salon, UUID staffMemberId, Instant start) {
        return body(salon.customerId(), salon, staffMemberId, start);
    }

    public static Map<String, Object> body(UUID customerId, TestSalon salon, UUID staffMemberId, Instant start) {
        var body = new HashMap<String, Object>();
        body.put("customerId", customerId);
        body.put("serviceId", salon.serviceId());
        body.put("staffMemberId", staffMemberId);
        body.put("start", start.toString());
        return body;
    }
}
