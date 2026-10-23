package com.takodu.api.shared.testing;

import io.restassured.http.ContentType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;

/**
 * A ready-to-book organization, built through the endpoints: its first establishment in {@code America/Lima} opens
 * 09:00-18:00 every day, and sells a 60-minute service for S/ 35.00 that the owner performs. Call it only from a
 * {@code @QuarkusTest}.
 *
 * @param serviceId the 60-minute service
 * @param customerId a registered customer
 */
public record TestSalon(TestTenant owner, UUID establishmentId, UUID serviceId, UUID customerId) {
    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    public static TestSalon open() {
        var owner = TestTenants.owner();
        var establishmentId = TestTenants.firstEstablishmentOf(owner);
        given().spec(owner.spec()).contentType(ContentType.JSON).body(Map.of("ranges", everyDay()))
                .put("/api/v1/establishments/" + establishmentId + "/opening-hours").then().statusCode(200);
        String serviceId = given().spec(owner.spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON).body(serviceBody(List.of(owner.memberId())))
                .post("/api/v1/establishments/" + establishmentId + "/services")
                .then().statusCode(201).extract().path("id");
        return new TestSalon(owner, establishmentId, UUID.fromString(serviceId), registerCustomer(owner));
    }

    /** @return a second establishment with the same opening hours, on the max plan (O4) */
    public UUID openSecondEstablishment() {
        TestTenants.upgradeToMax(owner);
        String id = given().spec(owner.spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON).body(Map.of("name", "Sede Surco"))
                .post("/api/v1/establishments").then().statusCode(201).extract().path("id");
        given().spec(owner.spec()).contentType(ContentType.JSON).body(Map.of("ranges", everyDay()))
                .put("/api/v1/establishments/" + id + "/opening-hours").then().statusCode(200);
        return UUID.fromString(id);
    }

    /** @return a staff member assigned to the establishment, not yet performing the service */
    public TestTenant hireStaff() {
        return TestTenants.staffOf(owner, establishmentId);
    }

    /** Replaces who performs the service; the team order still decides who comes first. */
    public void performedBy(TestTenant... members) {
        var ids = Arrays.stream(members).map(TestTenant::memberId).toList();
        given().spec(owner.spec()).contentType(ContentType.JSON).body(serviceBody(ids))
                .put("/api/v1/services/" + serviceId).then().statusCode(200);
    }

    /** @return a newly registered customer with a random Peruvian mobile */
    public UUID newCustomer() {
        return registerCustomer(owner);
    }

    private static UUID registerCustomer(TestTenant owner) {
        var phone = "9" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        String id = given().spec(owner.spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON).body(Map.of("phone", phone, "name", "Customer " + phone))
                .post("/api/v1/customers").then().statusCode(201).extract().path("id");
        return UUID.fromString(id);
    }

    /**
     * @param daysAhead days after today in Lima, by the given clock
     * @param localTime Lima time, such as {@code "10:00"}
     */
    public static Instant at(Clock clock, int daysAhead, String localTime) {
        return day(clock, daysAhead).atTime(LocalTime.parse(localTime)).atZone(LIMA).toInstant();
    }

    /** @return today in Lima by the given clock, plus {@code daysAhead} */
    public static LocalDate day(Clock clock, int daysAhead) {
        return LocalDate.ofInstant(clock.instant(), LIMA).plusDays(daysAhead);
    }

    private static List<Map<String, Object>> everyDay() {
        return IntStream.rangeClosed(1, 7)
                .mapToObj(day -> Map.<String, Object>of("weekday", day, "opensAt", "09:00", "closesAt", "18:00"))
                .toList();
    }

    private static Map<String, Object> serviceBody(List<UUID> performerIds) {
        var body = new HashMap<String, Object>();
        body.put("name", "Corte");
        body.put("durationMinutes", 60);
        body.put("priceCents", 3500);
        body.put("performerIds", performerIds);
        return body;
    }
}
