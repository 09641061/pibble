package com.takodu.api.organization.application.acl;

import com.takodu.api.organization.interfaces.acl.OpeningRangeView;
import com.takodu.api.organization.interfaces.acl.OrganizationContextFacade;
import com.takodu.api.organization.interfaces.acl.ServiceView;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OrganizationContextFacadeTest {

    @Inject
    OrganizationContextFacade organizationContextFacade;

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void calendarCarriesTimeZoneHoursAndPlan() {
        var salon = TestSalon.open();

        var calendar = organizationContextFacade.fetchCalendar(salon.owner().organizationId(), salon.establishmentId())
                .orElseThrow();

        assertEquals("America/Lima", calendar.timeZone());
        assertEquals(7, calendar.openingHours().size());
        assertEquals(new OpeningRangeView(1, LocalTime.of(9, 0), LocalTime.of(18, 0)), calendar.openingHours().getFirst());
        assertFalse(calendar.archived());
        assertTrue(calendar.planActive());
    }

    @Test
    void calendarShowsAnExpiredPlan() {
        var salon = TestSalon.open();
        clock.advance(Duration.ofDays(15));

        assertFalse(organizationContextFacade.fetchCalendar(salon.owner().organizationId(), salon.establishmentId())
                .orElseThrow().planActive());
    }

    @Test
    void performersComeInTheOrderTheyJoined() {
        var salon = TestSalon.open();
        var first = salon.hireStaff();
        var second = salon.hireStaff();
        salon.performedBy(second, salon.owner(), first);

        var service = organizationContextFacade.fetchService(salon.owner().organizationId(), salon.serviceId()).orElseThrow();

        assertEquals(List.of(salon.owner().memberId(), first.memberId(), second.memberId()), service.performerIds());
        assertEquals(60, service.durationMinutes());
        assertEquals(3500, service.priceCents());
        assertEquals("PEN", service.currency());
    }

    @Test
    void performerWhoStopsBeingBookableIsLeftOut() {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        salon.performedBy(salon.owner(), staff);
        given().spec(salon.owner().spec()).contentType("application/json")
                .body("{\"displayName\":\"Pausing\",\"bookable\":false,\"role\":\"staff\",\"establishmentIds\":[\"%s\"]}"
                        .formatted(salon.establishmentId()))
                .put("/api/v1/members/" + staff.memberId()).then().statusCode(200);

        var service = organizationContextFacade.fetchService(salon.owner().organizationId(), salon.serviceId()).orElseThrow();

        assertEquals(List.of(salon.owner().memberId()), service.performerIds());
    }

    @Test
    void memberWorksOnlyWhereAssigned() {
        var owner = TestTenants.owner();
        TestTenants.upgradeToMax(owner);
        var first = TestTenants.firstEstablishmentOf(owner);
        String second = given().spec(owner.spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json").body("{\"name\":\"Sede Surco\"}")
                .post("/api/v1/establishments").then().statusCode(201).extract().path("id");
        var staff = TestTenants.staffOf(owner, first);

        assertTrue(organizationContextFacade.fetchMember(owner.organizationId(), staff.memberId(), first).orElseThrow().worksThere());
        assertFalse(organizationContextFacade.fetchMember(owner.organizationId(), staff.memberId(), UUID.fromString(second))
                .orElseThrow().worksThere());
        assertTrue(organizationContextFacade.fetchMember(owner.organizationId(), owner.memberId(), UUID.fromString(second))
                .orElseThrow().worksThere());
    }

    @Test
    void activeServicesComeByNameWithTheirInstructions() {
        var salon = TestSalon.open();
        var organizationId = salon.owner().organizationId();
        given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Barba", "durationMinutes", 30, "priceCents", 2000,
                        "instructions", "Llega 5 minutos antes", "performerIds", List.of(salon.owner().memberId())))
                .post("/api/v1/establishments/" + salon.establishmentId() + "/services").then().statusCode(201);
        String inactive = given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Tinte", "durationMinutes", 90, "priceCents", 9000, "performerIds", List.of()))
                .post("/api/v1/establishments/" + salon.establishmentId() + "/services")
                .then().statusCode(201).extract().path("id");
        given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .post("/api/v1/services/" + inactive + "/deactivations").then().statusCode(200);

        var services = organizationContextFacade.fetchActiveServices(organizationId, salon.establishmentId());

        assertEquals(List.of("Barba", "Corte"), services.stream().map(ServiceView::name).toList());
        assertEquals("Llega 5 minutos antes", services.getFirst().instructions());
        assertEquals(List.of(salon.owner().memberId()), services.getFirst().performerIds());
        assertTrue(organizationContextFacade.fetchActiveServices(TestTenants.owner().organizationId(),
                salon.establishmentId()).isEmpty());
    }

    @Test
    void nothingIsFoundInAnotherOrganization() {
        var salon = TestSalon.open();
        var other = TestTenants.owner().organizationId();

        assertTrue(organizationContextFacade.fetchCalendar(other, salon.establishmentId()).isEmpty());
        assertTrue(organizationContextFacade.fetchService(other, salon.serviceId()).isEmpty());
        assertTrue(organizationContextFacade.fetchMember(other, salon.owner().memberId(), salon.establishmentId()).isEmpty());
    }
}
