package com.takodu.api.assistant.interfaces.rest;

import com.takodu.api.assistant.AssistantRows;
import com.takodu.api.conversations.TestInbox;
import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestOperators;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class AssistantUsageControllersTest {

    @Inject
    TestClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void ownerSeesAnsweredTurnsByLocalDay() {
        var salon = salonWithTurns();

        usage(salon.owner().spec(), salon.establishmentId(), "2026-10-01", "2026-10-01").statusCode(200)
                .body("timeZone", equalTo("America/Lima"))
                .body("totals.turns", equalTo(2))
                .body("totals.inputTokens", equalTo(3000))
                .body("totals.cachedTokens", equalTo(500))
                .body("totals.outputTokens", equalTo(300))
                .body("totals.toolCalls", equalTo(3))
                .body("days", hasSize(1));
        usage(salon.owner().spec(), salon.establishmentId(), "2026-09-30", "2026-10-01").statusCode(200)
                .body("totals.turns", equalTo(3))
                .body("days.date", equalTo(List.of("2026-09-30", "2026-10-01")))
                .body("days[0].totals.turns", equalTo(1));
    }

    @Test
    void managerReadsItWhileThePlanIsInactive() {
        var salon = salonWithTurns();
        var manager = TestTenants.managerOf(salon.owner());
        clock.advance(Duration.ofDays(15));

        usage(manager.spec(), salon.establishmentId(), "2026-09-30", "2026-10-01").statusCode(200)
                .body("totals.turns", equalTo(3));
    }

    @Test
    void staffCannotReadUsage() {
        var salon = TestSalon.open();

        usage(salon.hireStaff().spec(), salon.establishmentId(), "2026-10-01", "2026-10-31")
                .statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void anotherOrganizationsEstablishmentIsNotFound() {
        var salon = TestSalon.open();

        usage(TestTenants.owner().spec(), salon.establishmentId(), "2026-10-01", "2026-10-31").statusCode(404);
    }

    @Test
    void aMalformedOrTooLongPeriodIsRefused() {
        var salon = TestSalon.open();

        usage(salon.owner().spec(), salon.establishmentId(), "2026-10-31", "2026-10-01").statusCode(400);
        usage(salon.owner().spec(), salon.establishmentId(), "2025-01-01", "2026-10-01").statusCode(400);
        usage(salon.owner().spec(), salon.establishmentId(), "October", "2026-10-01").statusCode(400);
    }

    @Test
    void operatorSeesTheOrganizationsTotals() {
        var salon = salonWithTurns();

        given().spec(TestOperators.operator().spec())
                .get(internal(salon.owner().organizationId(), "2026-09-30", "2026-10-01")).then().statusCode(200)
                .body("totals.turns", equalTo(3))
                .body("totals.inputTokens", equalTo(4000))
                .body("establishments", hasSize(1))
                .body("establishments[0].establishmentId", equalTo(salon.establishmentId().toString()));
    }

    @Test
    void internalUsageIsOnlyForOperators() {
        var salon = TestSalon.open();
        var path = internal(salon.owner().organizationId(), "2026-10-01", "2026-10-31");

        given().get(path).then().statusCode(401);
        given().spec(salon.owner().spec()).get(path).then().statusCode(403);
        given().spec(TestOperators.operator().spec()).get(internal(UUID.randomUUID(), "2026-10-01", "2026-10-31"))
                .then().statusCode(404);
    }

    /**
     * Lima is UTC-5: 04:30Z on 1 October is still 30 September there. A skipped turn records no usage and is not
     * counted.
     */
    private TestSalon salonWithTurns() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var waId = "51987" + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        UUID conversation = null;
        for (int i = 0; i < 4; i++)
            conversation = TestInbox.customerWrites(salon.owner(), salon.establishmentId(), line, waId, clock.instant());
        AssistantRows.recordTurn(conversation, "done", Instant.parse("2026-10-01T04:30:00Z"), 1000, 0, 100, 0);
        AssistantRows.recordTurn(conversation, "done", Instant.parse("2026-10-01T05:30:00Z"), 1200, 500, 120, 2);
        AssistantRows.recordTurn(conversation, "done", Instant.parse("2026-10-02T04:59:00Z"), 1800, 0, 180, 1);
        AssistantRows.recordTurn(conversation, "skipped", Instant.parse("2026-10-01T12:00:00Z"), 0, 0, 0, 0);
        return salon;
    }

    private static ValidatableResponse usage(RequestSpecification spec, UUID establishmentId, String from, String to) {
        return given().spec(spec).queryParam("from", from).queryParam("to", to)
                .get("/api/v1/establishments/" + establishmentId + "/assistant/usage").then();
    }

    private static String internal(UUID organizationId, String from, String to) {
        return "/internal/v1/organizations/" + organizationId + "/assistant-usage?from=" + from + "&to=" + to;
    }
}
