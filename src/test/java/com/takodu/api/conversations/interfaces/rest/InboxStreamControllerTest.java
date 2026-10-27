package com.takodu.api.conversations.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.takodu.api.assistant.AssistantRows;
import com.takodu.api.assistant.TestAssistant;
import com.takodu.api.assistant.application.internal.jobs.TurnWorker;
import com.takodu.api.assistant.infrastructure.llm.ScriptedChatModel;
import com.takodu.api.booking.interfaces.rest.AppointmentsControllerTest;
import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.takodu.api.assistant.infrastructure.llm.ScriptedChatModel.reply;
import static com.takodu.api.conversations.MetaWebhooks.newWaId;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class InboxStreamControllerTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TestHTTPResource("/api/v1/inbox/stream")
    URI stream;

    @Inject
    TurnWorker worker;

    @Inject
    ScriptedChatModel model;

    @Test
    void staffFollowOnlyTheEstablishmentsTheySee() throws Exception {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();
        var miraflores = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var surcoLine = TestWhatsapp.connectLine(salon.owner(), surco);
        var staff = salon.hireStaff();
        var other = TestSalon.open();
        var otherLine = TestWhatsapp.connectLine(other.owner(), other.establishmentId());

        try (var events = Events.follow(stream, staff)) {
            customerWrites(salon.owner(), surco, surcoLine, newWaId(), Instant.now());
            customerWrites(other.owner(), other.establishmentId(), otherLine, newWaId(), Instant.now());
            var conversation = customerWrites(salon.owner(), salon.establishmentId(), miraflores, newWaId(), Instant.now());

            assertEquals(conversation.toString(), events.nextConversation());
        }
    }

    @Test
    void readingAndReplyingReachOtherMembers() throws Exception {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());
        var manager = TestTenants.managerOf(salon.owner());

        try (var events = Events.follow(stream, manager)) {
            given().spec(salon.owner().spec()).post("/api/v1/conversations/" + conversation + "/read")
                    .then().statusCode(200);

            assertEquals(conversation.toString(), events.nextConversation());
        }
    }

    @Test
    void aChangeCarriesTheConversationsSummary() throws Exception {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var wroteAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        try (var events = Events.follow(stream, salon.owner())) {
            var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), wroteAt);

            var change = events.next();
            assertEquals("conversation", change.get("type").asText());
            assertEquals(conversation.toString(), change.get("conversationId").asText());
            assertEquals(salon.establishmentId().toString(), change.get("establishmentId").asText());
            var summary = change.get("conversation");
            assertEquals(1, summary.get("unreadCount").asInt());
            assertEquals(wroteAt, Instant.parse(summary.get("lastInboundAt").asText()));
            assertEquals(wroteAt, Instant.parse(summary.get("lastMessageAt").asText()));
            assertEquals(wroteAt.plus(Duration.ofHours(24)), Instant.parse(summary.get("windowClosesAt").asText()));
            assertFalse(summary.get("assistantHandedOff").asBoolean());
            assertFalse(summary.get("assistantPausedManually").asBoolean());
        }
    }

    @Test
    void theAssistantTypesWhileItsTurnRuns() throws Exception {
        AssistantRows.clearQueue();
        var salon = TestSalon.open();
        TestAssistant.enable(salon.owner(), salon.establishmentId(), "");
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());
        model.script(reply("¡Hola! ¿En qué te ayudo?"));

        try (var events = Events.follow(stream, salon.owner())) {
            worker.runDue();

            var seen = new ArrayList<String>();
            for (int i = 0; i < 3; i++) {
                var event = events.next();
                assertEquals(conversation.toString(), event.get("conversationId").asText());
                seen.add(event.get("type").asText());
            }
            assertEquals(List.of("assistant_typing", "conversation", "assistant_idle"), seen);
        }
    }

    @Test
    void calendarsHearOfNewAppointmentsInTheirEstablishments() throws Exception {
        var salon = TestSalon.open();
        var staff = salon.hireStaff();
        var start = java.time.LocalDate.now(TestSalon.LIMA).plusDays(2).atTime(10, 0).atZone(TestSalon.LIMA)
                .toInstant();

        try (var events = Events.follow(stream, staff)) {
            String appointmentId = AppointmentsControllerTest.book(salon.owner(), UUID.randomUUID(),
                    AppointmentsControllerTest.body(salon, salon.owner().memberId(), start))
                    .statusCode(201).extract().path("id");

            var event = events.next();
            assertEquals("appointment_booked", event.get("type").asText());
            assertEquals(appointmentId, event.get("appointmentId").asText());
            assertEquals(salon.establishmentId().toString(), event.get("establishmentId").asText());
        }
    }

    @Test
    void streamNeedsAMembership() {
        given().header("Authorization", "Bearer " + TestTenants.outsider())
                .header("X-Organization-Id", UUID.randomUUID().toString())
                .get("/api/v1/inbox/stream").then().statusCode(403);
    }

    /** Reads the stream's data lines on a background thread. */
    private record Events(CompletableFuture<HttpResponse<Stream<String>>> response, BlockingQueue<String> lines)
            implements AutoCloseable {

        /** @return once the first ping arrived, so the stream is subscribed */
        static Events follow(URI stream, TestTenant tenant) throws Exception {
            var lines = new LinkedBlockingQueue<String>();
            var request = HttpRequest.newBuilder(stream)
                    .header("Authorization", "Bearer " + tenant.accessToken())
                    .header("X-Organization-Id", tenant.organizationId().toString())
                    .header("Accept", "text/event-stream").build();
            var response = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofLines());
            response.thenAccept(r -> CompletableFuture.runAsync(() -> r.body()
                    .filter(line -> line.startsWith("data:"))
                    .forEach(lines::add)));
            var events = new Events(response, lines);
            var first = lines.poll(10, TimeUnit.SECONDS);
            assertNotNull(first, "no ping after connecting");
            assertTrue(first.contains("\"ping\""), first);
            return events;
        }

        /** @return the id of the next conversation change, skipping pings */
        String nextConversation() throws Exception {
            return next().get("conversationId").asText();
        }

        /** @return the next event that is not a ping */
        JsonNode next() throws Exception {
            var deadline = Instant.now().plus(Duration.ofSeconds(10));
            while (Instant.now().isBefore(deadline)) {
                var line = lines.poll(1, TimeUnit.SECONDS);
                if (line == null || line.contains("\"ping\"")) continue;
                return JSON.readTree(line.substring("data:".length()));
            }
            throw new AssertionError("No inbox event arrived");
        }

        @Override
        public void close() {
            response.thenAccept(r -> r.body().close());
            response.cancel(true);
        }
    }
}
