package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.takodu.api.conversations.MetaWebhooks.newWaId;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class InboxStreamControllerTest {

    @TestHTTPResource("/api/v1/inbox/stream")
    URI stream;

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
        String nextConversation() throws InterruptedException {
            var deadline = Instant.now().plus(Duration.ofSeconds(10));
            while (Instant.now().isBefore(deadline)) {
                var line = lines.poll(1, TimeUnit.SECONDS);
                if (line == null || line.contains("\"ping\"")) continue;
                return line.replaceAll(".*\"conversationId\":\"([^\"]+)\".*", "$1");
            }
            throw new AssertionError("No conversation change arrived");
        }

        @Override
        public void close() {
            response.thenAccept(r -> r.body().close());
            response.cancel(true);
        }
    }
}
