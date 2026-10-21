package com.takodu.api.conversations.interfaces.rest;

import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.shared.testing.TestSalon;
import com.takodu.api.shared.testing.TestTenant;
import com.takodu.api.shared.testing.TestTenants;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.takodu.api.conversations.MetaWebhooks.newWaId;
import static com.takodu.api.conversations.TestInbox.conversation;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class ConversationsControllerTest {

    @Test
    void inboxShowsNewestActivityFirstWithCustomerWindowAndUnread() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var rosa = newWaId();
        var luis = newWaId();
        var now = Instant.now();
        customerWrites(salon.owner(), salon.establishmentId(), line, rosa, now.minus(Duration.ofHours(30)));
        customerWrites(salon.owner(), salon.establishmentId(), line, luis, now.minusSeconds(60));

        list(salon.hireStaff(), salon.establishmentId(), Map.of()).statusCode(200)
                .body("items", hasSize(2))
                .body("items[0].customerPhone", equalTo("+" + luis))
                .body("items[0].customerName", equalTo("Rosa Quispe"))
                .body("items[0].unreadCount", equalTo(1))
                .body("items[0].windowOpen", equalTo(true))
                .body("items[0].windowClosesAt", notNullValue())
                .body("items[0].assistantPaused", equalTo(false))
                .body("items[1].customerPhone", equalTo("+" + rosa))
                .body("items[1].windowOpen", equalTo(false))
                .body("nextCursor", nullValue());
    }

    @Test
    void inboxIsPagedWithACursor() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        for (int i = 0; i < 3; i++)
            customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now().minusSeconds(10L * i));

        String cursor = list(salon.owner(), salon.establishmentId(), Map.of("size", 2))
                .body("items", hasSize(2)).extract().path("nextCursor");
        list(salon.owner(), salon.establishmentId(), Map.of("size", 2, "cursor", cursor))
                .body("items", hasSize(1)).body("nextCursor", nullValue());
        list(salon.owner(), salon.establishmentId(), Map.of("cursor", "not-a-cursor")).statusCode(400);
        list(salon.owner(), salon.establishmentId(), Map.of("size", 51)).statusCode(400);
    }

    @Test
    void markingReadResetsUnread() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());

        read(salon.hireStaff(), conversation).statusCode(200).body("unreadCount", equalTo(0));
        read(salon.owner(), conversation).statusCode(200).body("unreadCount", equalTo(0));
    }

    @Test
    void conversationIsAssignedToSomeoneWhoSeesTheInbox() {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());
        var staff = salon.hireStaff();
        var surcoStaff = TestTenants.staffOf(salon.owner(), surco);

        assign(staff, conversation, staff.memberId()).statusCode(200)
                .body("assignedMemberId", equalTo(staff.memberId().toString()));
        assign(salon.owner(), conversation, surcoStaff.memberId()).statusCode(422)
                .body("code", equalTo("MEMBER_NOT_AT_ESTABLISHMENT"));
        assign(salon.owner(), conversation, UUID.randomUUID()).statusCode(404).body("code", equalTo("MEMBER_NOT_FOUND"));
        assign(salon.owner(), conversation, null).statusCode(200).body("assignedMemberId", nullValue());
    }

    @Test
    void staffOfAnotherEstablishmentCannotSeeOrTouchItsInbox() {
        var salon = TestSalon.open();
        var surco = salon.openSecondEstablishment();
        var line = TestWhatsapp.connectLine(salon.owner(), surco);
        var conversation = customerWrites(salon.owner(), surco, line, newWaId(), Instant.now());
        var staff = salon.hireStaff();

        list(staff, surco, Map.of()).statusCode(403).body("code", equalTo("ACCESS_DENIED"));
        conversation(staff, conversation).statusCode(403);
        read(staff, conversation).statusCode(403);
        assign(staff, conversation, staff.memberId()).statusCode(403);
    }

    @Test
    void anotherOrganizationsInboxIsNotFound() {
        var salon = TestSalon.open();
        var line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        var conversation = customerWrites(salon.owner(), salon.establishmentId(), line, newWaId(), Instant.now());
        var stranger = TestTenants.owner();

        list(stranger, salon.establishmentId(), Map.of()).statusCode(404);
        conversation(stranger, conversation).statusCode(404).body("code", equalTo("CONVERSATION_NOT_FOUND"));
        read(stranger, conversation).statusCode(404);
        assign(stranger, conversation, stranger.memberId()).statusCode(404);
    }

    static ValidatableResponse list(TestTenant tenant, UUID establishmentId, Map<String, Object> parameters) {
        return given().spec(tenant.spec()).queryParams(parameters)
                .get("/api/v1/establishments/" + establishmentId + "/conversations").then();
    }

    private static ValidatableResponse read(TestTenant tenant, UUID conversationId) {
        return given().spec(tenant.spec()).post("/api/v1/conversations/" + conversationId + "/read").then();
    }

    private static ValidatableResponse assign(TestTenant tenant, UUID conversationId, UUID memberId) {
        var body = new HashMap<String, Object>();
        body.put("memberId", memberId);
        return given().spec(tenant.spec()).contentType(ContentType.JSON).body(body)
                .put("/api/v1/conversations/" + conversationId + "/assignee").then();
    }
}
