package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.interfaces.acl.ConversationsContextFacade;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.takodu.api.conversations.MetaWebhooks.newWaId;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static com.takodu.api.conversations.TestInbox.reply;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class InboxSummaryControllerTest {

    @Inject
    ConversationsContextFacade conversations;

    @Test
    void countsTheChatsThatWaitForAPersonAcrossTheWholeInbox() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var organizationId = salon.owner().organizationId();
        // Waiting: the customer wrote and nobody answered.
        customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());
        // Unread but answered: the assistant replied after the customer.
        var answered = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());
        conversations.sendAssistantReply(organizationId, answered, "¡Hola!", null, UUID.randomUUID());
        // Waiting: a person replied, then the assistant handed the chat off.
        var handedOff = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());
        reply(salon.owner(), handedOff, UUID.randomUUID(), "Te ayudo").statusCode(201);
        conversations.handOff(organizationId, handedOff, "Quiere hablar con el dueño");
        // Read and answered by a person: neither waiting nor unread.
        var read = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());
        reply(salon.owner(), read, UUID.randomUUID(), "Listo").statusCode(201);

        summary(salon.hireStaff(), salon.establishmentId()).statusCode(200)
                .body("awaitingReply", equalTo(2))
                .body("unread", equalTo(3));
    }

    @Test
    void staffSeeOnlyTheirEstablishments() {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();

        summary(salon.hireStaff(), surco).statusCode(403).body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    void anotherOrganizationsEstablishmentIsNotFound() {
        var salon = TestSalon.open();

        summary(salon.owner(), TestSalon.open().establishmentId()).statusCode(404);
    }

    @Test
    void theEstablishmentIsRequired() {
        given().spec(TestSalon.open().owner().spec()).get("/api/v1/inbox/summary").then().statusCode(400);
    }

    private static ValidatableResponse summary(TestTenant tenant, UUID establishmentId) {
        return given().spec(tenant.spec()).queryParam("establishmentId", establishmentId.toString())
                .get("/api/v1/inbox/summary").then();
    }
}
