package com.takodu.api.assistant.application.internal.jobs;

import com.takodu.api.assistant.AssistantRows;
import com.takodu.api.assistant.TestAssistant;
import com.takodu.api.assistant.infrastructure.llm.ScriptedChatModel;
import com.takodu.api.conversations.MetaWebhooks;
import com.takodu.api.conversations.TestWhatsapp;
import com.takodu.api.conversations.application.internal.jobs.OutboundMessageSender;
import com.takodu.api.conversations.application.internal.outboundservices.whatsapp.RecordingWhatsappGateway;
import com.takodu.api.shared.infrastructure.time.TestClock;
import com.takodu.api.shared.testing.TestSalon;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.takodu.api.assistant.AssistantRows.turnsOf;
import static com.takodu.api.assistant.infrastructure.llm.ScriptedChatModel.failure;
import static com.takodu.api.assistant.infrastructure.llm.ScriptedChatModel.reply;
import static com.takodu.api.assistant.infrastructure.llm.ScriptedChatModel.tool;
import static com.takodu.api.assistant.infrastructure.llm.ScriptedChatModel.toolResults;
import static com.takodu.api.conversations.ConversationRows.notificationsOf;
import static com.takodu.api.conversations.TestInbox.conversation;
import static com.takodu.api.conversations.TestInbox.customerWrites;
import static com.takodu.api.conversations.TestInbox.messages;
import static com.takodu.api.conversations.TestInbox.reply;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Turns run end to end: the real webhook queues them, a scripted model calls the real tools, booking books, and the
 * outbox sends the reply. No test calls a language model.
 */
@QuarkusTest
class TurnWorkerTest {

    @Inject
    TestClock clock;

    @Inject
    TurnWorker worker;

    @Inject
    OutboundMessageSender sender;

    @Inject
    ScriptedChatModel model;

    @Inject
    RecordingWhatsappGateway gateway;

    private TestSalon salon;
    private String line;
    private String waId;

    @BeforeEach
    void salonWithTheAssistantOn() {
        AssistantRows.clearQueue();
        clock.set(TestSalon.at(clock, 1, "08:00"));
        salon = TestSalon.open();
        TestAssistant.enable(salon.owner(), salon.establishmentId(), "Aceptamos Yape y Plin.");
        line = TestWhatsapp.connectLine(salon.owner(), salon.establishmentId());
        waId = MetaWebhooks.newWaId();
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void assistantBooksThroughBookingAndRepliesOnce() {
        var conversationId = customerSays("¿Tienen cita mañana a las 10 para corte?");
        model.script(
                tool("findSlots", "{\"serviceName\": \"corte\", \"date\": \"%s\"}".formatted(day(1))),
                tool("bookAppointment", "{\"serviceName\": \"Corte\", \"start\": \"%sT10:00\"}".formatted(day(1))),
                reply("Listo, te esperamos mañana a las 10:00."));

        run();

        assertEquals(List.of("Listo, te esperamos mañana a las 10:00."), gateway.sentTo(waId));
        var results = toolResults(model.requests().getLast());
        assertTrue(results.getFirst().startsWith("Horarios libres el "), results.getFirst());
        assertTrue(results.getLast().startsWith("Cita agendada: Corte con "), results.getLast());
        var appointment = onlyAppointment();
        assertEquals("assistant", appointment.get("source"));
        assertEquals(TestSalon.at(clock, 1, "10:00").toString(), appointment.get("start"));
        assertEquals(java.util.Set.of("reminder"),
                notificationsOf(UUID.fromString((String) appointment.get("id"))).keySet());
        var turn = turnsOf(conversationId).getFirst();
        assertEquals("done", turn.get("status"));
        assertEquals(ConfigProvider.getConfig().getValue("quarkus.langchain4j.openai.chat-model.model-name",
                String.class), turn.get("model"));
        assertEquals(300, turn.get("input_tokens"));
        assertEquals(120, turn.get("cached_tokens"));
        assertEquals(60, turn.get("output_tokens"));
        assertEquals((short) 2, turn.get("tool_calls"));
        messages(salon.owner(), conversationId)
                .body("items.find { it.origin == 'assistant' }.status", equalTo("sent"));
    }

    @Test
    void shortChoicesGoAsButtonsAndATapIsTheNextMessageTheModelReads() {
        customerSays("¿Tienen cita mañana para corte?");
        clock.advance(Duration.ofSeconds(5));
        model.script(reply("¿A qué hora te acomoda?", "10:00", "10:30", "11:00"));
        run();
        assertEquals(List.of("buttons(¿A qué hora te acomoda?|10:00|10:30|11:00)"), gateway.sentTo(waId));

        clock.advance(Duration.ofMinutes(1));
        customerTaps("button-reply", "10:30");
        model.script(reply("Perfecto."));
        run();

        var messages = model.lastMessages();
        var asked = ((UserMessage) messages.getLast()).singleText();
        assertTrue(asked.startsWith("10:30\n"), asked);
        assertTrue(messages.stream().anyMatch(message -> message instanceof AiMessage ai
                && "¿A qué hora te acomoda?\n1. 10:00\n2. 10:30\n3. 11:00".equals(ai.text())), messages::toString);
    }

    @Test
    void moreOrLongerChoicesGoAsAListAndRowsAreTapped() {
        customerSays("¿Qué servicios tienen?");
        clock.advance(Duration.ofSeconds(5));
        model.script(reply("Tenemos estos servicios:", "Corte", "Tinte", "Manicure", "Pedicure"));
        run();
        assertEquals(List.of("list(Tenemos estos servicios:|Ver opciones|Corte|Tinte|Manicure|Pedicure)"),
                gateway.sentTo(waId));

        clock.advance(Duration.ofMinutes(1));
        customerTaps("list-reply", "Manicure");
        model.script(reply("¿Para qué día?"));
        run();

        var asked = ((UserMessage) model.lastMessages().getLast()).singleText();
        assertTrue(asked.startsWith("Manicure\n"), asked);
    }

    @Test
    void modelNeverSeesTheCustomersPhone() {
        customerSays("Hola");
        model.script(reply("¡Hola! ¿En qué te ayudo?"));

        run();

        assertFalse(model.requests().toString().contains(waId.substring(2)));
    }

    @Test
    void retriedTurnBooksOnce() {
        var conversationId = customerSays("Corte mañana a las 11");
        var book = tool("bookAppointment", "{\"serviceName\": \"Corte\", \"start\": \"%sT11:00\"}".formatted(day(1)));
        model.script(book, failure());
        run();
        assertEquals("pending", turnsOf(conversationId).getFirst().get("status"));

        clock.advance(Duration.ofSeconds(30));
        model.script(book, reply("Listo."));
        run();

        assertEquals(1, appointments().size());
        var turn = turnsOf(conversationId).getFirst();
        assertEquals("done", turn.get("status"));
        assertEquals((short) 2, turn.get("attempts"));
        assertEquals(List.of("Listo."), gateway.sentTo(waId));
    }

    @Test
    void bookingTwentyMinutesAheadIsRefusedByBooking() {
        clock.set(TestSalon.at(clock, 0, "09:40"));
        customerSays("¿Puedo ir a las 10?");
        model.script(
                tool("bookAppointment", "{\"serviceName\": \"Corte\", \"start\": \"%sT10:00\"}".formatted(day(0))),
                reply("Necesito al menos 30 minutos. ¿Te sirve a las 10:15?", "10:15", "10:30"));

        run();

        var result = toolResults(model.requests().getLast()).getFirst();
        assertTrue(result.startsWith("Solo puedo agendar con al menos 30 minutos"), result);
        assertTrue(result.contains("10:15"), result);
        assertEquals(List.of(), appointments());
        assertEquals(List.of("buttons(Necesito al menos 30 minutos. ¿Te sirve a las 10:15?|10:15|10:30)"),
                gateway.sentTo(waId));
    }

    @Test
    void assistantReschedulesAndCancelsTheCustomersAppointment() {
        var appointmentId = staffBooks(TestSalon.at(clock, 2, "10:00"));
        var conversationId = customerSays("Quiero mover mi cita");
        model.script(
                tool("myAppointments", "{}"),
                tool("rescheduleAppointment", "{\"appointmentNumber\": 1, \"newStart\": \"%sT15:00\"}"
                        .formatted(day(2))),
                reply("Listo, la moví a las 15:00."));
        run();
        assertEquals(TestSalon.at(clock, 2, "15:00").toString(), appointment(appointmentId).get("start"));

        clock.advance(Duration.ofMinutes(5));
        customerSays("Mejor cancélala, no podré ir");
        model.script(tool("cancelAppointment", "{\"appointmentNumber\": 1, \"reason\": \"No podrá ir\"}"),
                reply("Listo, la cancelé."));
        run();

        var cancelled = appointment(appointmentId);
        assertEquals("cancelled", cancelled.get("status"));
        assertEquals("customer", cancelled.get("cancelledBy"));
        assertEquals(2, turnsOf(conversationId).stream().filter(turn -> "done".equals(turn.get("status"))).count());
    }

    @Test
    void changeWithinTwoHoursIsHandedToAPerson() {
        var appointmentId = staffBooks(TestSalon.at(clock, 0, "09:30"));
        var conversationId = customerSays("Cancela mi cita de hoy");
        model.script(tool("cancelAppointment", "{\"appointmentNumber\": 1, \"reason\": \"Se le complicó\"}"),
                reply("Una persona del equipo te ayudará."));

        run();

        assertEquals("confirmed", appointment(appointmentId).get("status"));
        conversation(salon.owner(), conversationId).body("assistantHandedOff", equalTo(true));
    }

    @Test
    void anotherEstablishmentsServiceDoesNotExistHere() {
        var other = TestSalon.open();
        given().spec(other.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Masaje", "durationMinutes", 60, "priceCents", 8000,
                        "performerIds", List.of(other.owner().memberId())))
                .post("/api/v1/establishments/" + other.establishmentId() + "/services").then().statusCode(201);
        customerSays("Quiero un masaje");
        model.script(
                tool("bookAppointment", "{\"serviceName\": \"Masaje\", \"start\": \"%sT10:00\"}".formatted(day(1))),
                reply("No tenemos masajes."));

        run();

        assertEquals("No existe ese servicio. Servicios: Corte.", toolResults(model.requests().getLast()).getFirst());
        assertEquals(List.of(), appointments());
    }

    @Test
    void severalQuickMessagesGetOneAnswer() {
        var conversationId = customerSays("Hola");
        clock.advance(Duration.ofSeconds(5));
        customerSays("¿Atienden el domingo?");
        model.script(reply("¡Hola! Sí, de 09:00 a 18:00."));

        run();

        var turns = turnsOf(conversationId);
        assertEquals(List.of("skipped", "done"), turns.stream().map(turn -> turn.get("status")).toList());
        assertEquals(1, model.requests().size());
        var asked = ((UserMessage) model.lastMessages().getLast()).singleText();
        assertTrue(asked.startsWith("Hola\n¿Atienden el domingo?"), asked);
        assertEquals(1, gateway.sentTo(waId).size());
    }

    @Test
    void messagesInTheSameSecondGetOneAnswer() {
        var conversationId = customerSays("Hola");
        customerSays("¿Atienden el domingo?");
        model.script(reply("¡Hola! Sí, de 09:00 a 18:00."));

        run();

        var statuses = turnsOf(conversationId).stream().map(turn -> turn.get("status")).sorted().toList();
        assertEquals(List.of("done", "skipped"), statuses);
        assertEquals(1, model.requests().size());
        assertEquals(1, gateway.sentTo(waId).size());
    }

    @Test
    void pausedDisabledOrClosedChatsGetNoAnswer() {
        var paused = customerSays("Hola");
        clock.advance(Duration.ofMinutes(1));
        reply(salon.owner(), paused, UUID.randomUUID(), "Te atiendo yo").statusCode(201);
        model.script();
        run();
        assertEquals("A person is handling the chat", turnsOf(paused).getFirst().get("error"));

        waId = MetaWebhooks.newWaId();
        var closed = customerSays("Hola");
        clock.advance(Duration.ofHours(25));
        run();
        assertEquals("The customer service window is closed", turnsOf(closed).getFirst().get("error"));

        waId = MetaWebhooks.newWaId();
        var off = customerSays("Hola");
        TestAssistant.disable(salon.owner(), salon.establishmentId());
        run();
        assertEquals("The assistant is off", turnsOf(off).getFirst().get("error"));
        assertEquals(List.of(), model.requests());
    }

    @Test
    void manuallyPausedChatGetsNoTurnUntilResumed() {
        var conversationId = customerSays("Hola");
        assistant(conversationId, "pause");
        run();
        assertEquals("A person is handling the chat", turnsOf(conversationId).getFirst().get("error"));

        clock.advance(Duration.ofHours(13));
        customerSays("¿Hay alguien?");
        assertEquals(1, turnsOf(conversationId).size());

        assistant(conversationId, "resume");
        clock.advance(Duration.ofMinutes(1));
        customerSays("Quiero una cita");
        clock.advance(Duration.ofSeconds(5));
        model.script(reply("¡Claro! ¿Para qué servicio?"));
        run();

        assertEquals(2, turnsOf(conversationId).size());
        assertEquals(List.of("¡Claro! ¿Para qué servicio?"), gateway.sentTo(waId));
    }

    @Test
    void resumingAHandedOffChatLetsTheAssistantAnswerAgain() {
        var conversationId = customerSays("Quiero hablar con alguien");
        clock.advance(Duration.ofSeconds(5));
        model.script(tool("handOffToHuman", "{\"summary\": \"Pide una persona\"}"), reply("Claro."));
        run();
        clock.advance(Duration.ofMinutes(1));
        customerSays("¿Hola?");
        assertEquals(1, turnsOf(conversationId).size());

        assistant(conversationId, "resume");
        clock.advance(Duration.ofMinutes(1));
        customerSays("Ya no, mejor agéndame");
        clock.advance(Duration.ofSeconds(5));
        model.script(reply("¿Qué servicio?"));
        run();

        assertEquals(List.of("Claro.", "¿Qué servicio?"), gateway.sentTo(waId));
    }

    @Test
    void threeFailuresHandTheChatToAPerson() {
        var conversationId = customerSays("Hola");
        for (int attempt = 1; attempt <= 3; attempt++) {
            model.script(failure());
            run();
            clock.advance(Duration.ofMinutes(2));
        }

        var turn = turnsOf(conversationId).getFirst();
        assertEquals("failed", turn.get("status"));
        assertEquals((short) 3, turn.get("attempts"));
        conversation(salon.owner(), conversationId).statusCode(200)
                .body("assistantHandedOff", equalTo(true))
                .body("handoffSummary", equalTo("El asistente no pudo responder después de 3 intentos"));
        assertEquals(List.of(), gateway.sentTo(waId));
    }

    @Test
    void customerWhoAsksForAPersonGetsOne() {
        var conversationId = customerSays("Quiero hablar con alguien");
        model.script(tool("handOffToHuman", "{\"summary\": \"Pide hablar con una persona\"}"),
                reply("Claro, una persona del equipo te escribirá."));

        run();

        assertEquals(List.of("Claro, una persona del equipo te escribirá."), gateway.sentTo(waId));
        conversation(salon.owner(), conversationId).statusCode(200)
                .body("assistantHandedOff", equalTo(true))
                .body("handoffSummary", equalTo("Pide hablar con una persona"))
                .body("unreadCount", equalTo(1));
    }

    @Test
    void voiceNoteIsHandedToAPersonWithoutTheModel() {
        customerSays("Hola");
        model.script(reply("¡Hola!"));
        run();
        MetaWebhooks.post(MetaWebhooks.payload("audio-message", Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId,
                "WAMID", MetaWebhooks.newWamid(), "TIMESTAMP", String.valueOf(clock.instant().getEpochSecond() + 60))))
                .statusCode(200);
        model.script();

        run();

        assertEquals(List.of("¡Hola!", "Por ahora solo puedo leer mensajes de texto. Una persona del equipo"
                + " continuará la conversación."), gateway.sentTo(waId));
        assertEquals(List.of(), model.requests());
    }

    private UUID customerSays(String body) {
        return customerWrites(salon.owner(), salon.establishmentId(), line, waId, clock.instant(), body);
    }

    private void customerTaps(String payload, String title) {
        MetaWebhooks.post(MetaWebhooks.payload(payload, Map.of("PHONE_NUMBER_ID", line, "WA_ID", waId,
                "WAMID", MetaWebhooks.newWamid(), "TIMESTAMP", String.valueOf(clock.instant().getEpochSecond()),
                "BODY", title))).statusCode(200);
    }

    private void assistant(UUID conversationId, String action) {
        given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .post("/api/v1/conversations/" + conversationId + "/assistant/" + action).then().statusCode(200);
    }

    private void run() {
        worker.runDue();
        sender.sendDue();
    }

    private String day(int daysAhead) {
        return TestSalon.day(clock, daysAhead).toString();
    }

    private UUID staffBooks(Instant start) {
        String id = given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body(Map.of("customerId", customerOf(), "serviceId", salon.serviceId(), "start", start.toString()))
                .post("/api/v1/appointments").then().statusCode(201).extract().path("id");
        return UUID.fromString(id);
    }

    /** The customer behind {@link #waId}, registered the way staff do, so the webhook finds them. */
    private String customerOf() {
        String id = given().spec(salon.owner().spec()).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON).body(Map.of("phone", waId.substring(2), "name", "Rosa"))
                .post("/api/v1/customers").then().statusCode(201).extract().path("id");
        return id;
    }

    private Map<String, Object> appointment(UUID appointmentId) {
        return given().spec(salon.owner().spec()).get("/api/v1/appointments/" + appointmentId)
                .then().statusCode(200).extract().as(Map.class);
    }

    private List<Map<String, Object>> appointments() {
        return given().spec(salon.owner().spec()).queryParam("from", clock.instant().toString())
                .queryParam("to", clock.instant().plus(Duration.ofDays(7)).toString())
                .get("/api/v1/establishments/" + salon.establishmentId() + "/appointments")
                .then().statusCode(200).extract().jsonPath().getList("$");
    }

    private Map<String, Object> onlyAppointment() {
        var all = appointments();
        assertEquals(1, all.size());
        return all.getFirst();
    }
}
