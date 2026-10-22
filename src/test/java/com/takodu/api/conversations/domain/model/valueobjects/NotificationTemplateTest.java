package com.takodu.api.conversations.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationTemplateTest {

    @ParameterizedTest
    @EnumSource(NotificationTemplate.class)
    void bodiesFollowMetasRules(NotificationTemplate template) {
        assertFalse(template.body().startsWith("{{"), "a body cannot start with a parameter");
        assertFalse(template.body().endsWith("}}"), "a body cannot end with a parameter");
        assertFalse(template.body().contains("}}{{") || template.body().contains("}} {{"),
                "parameters cannot be next to each other");
        assertEquals(template.parameterCount(), template.example().size());
        template.buttons().forEach(title -> assertFalse(title.length() > 20, "a quick reply title has at most 20"));
    }

    @Test
    void renderingFillsTheParametersAndFlattensWhitespace() {
        var text = NotificationTemplate.CANCELLATION.render(List.of("Ana", "Corte", "sábado 24 de octubre a las 10:00",
                "el local\n  cierra $1"));

        assertEquals("Hola Ana, tu cita de Corte del sábado 24 de octubre a las 10:00 fue cancelada: el local cierra $1. "
                + "Escríbenos para reprogramar.", text);
    }

    @Test
    void wrongOrBlankParametersAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> NotificationTemplate.REMINDER.render(List.of("Ana")));
        assertThrows(IllegalArgumentException.class,
                () -> NotificationTemplate.CANCELLATION.render(List.of("Ana", "Corte", " ", "x")));
    }
}
