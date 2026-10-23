package com.takodu.api.conversations.infrastructure.meta;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The interactive payloads in the shape of Meta's Cloud API docs for reply buttons and list messages. */
class MetaWhatsappGatewayTest {

    @Test
    void buttonsAreReplyButtonsWithPositionalIds() {
        assertEquals(Map.of("messaging_product", "whatsapp", "recipient_type", "individual", "to", "51987654321",
                "type", "interactive", "interactive", Map.of("type", "button", "body", Map.of("text", "¿Hora?"),
                        "action", Map.of("buttons", List.of(
                                Map.of("type", "reply", "reply", Map.of("id", "option-1", "title", "10:00")),
                                Map.of("type", "reply", "reply", Map.of("id", "option-2", "title", "11:00")))))),
                MetaWhatsappGateway.buttons("51987654321", "¿Hora?", List.of("10:00", "11:00")));
    }

    @Test
    void listHasOneTitledSectionBehindTheButtonLabel() {
        assertEquals(Map.of("messaging_product", "whatsapp", "recipient_type", "individual", "to", "51987654321",
                "type", "interactive", "interactive", Map.of("type", "list", "body", Map.of("text", "Elige"),
                        "action", Map.of("button", "Ver opciones", "sections", List.of(Map.of("title", "Opciones",
                                "rows", List.of(Map.of("id", "option-1", "title", "Corte"))))))),
                MetaWhatsappGateway.list("51987654321", "Elige", "Ver opciones", List.of("Corte")));
    }
}
