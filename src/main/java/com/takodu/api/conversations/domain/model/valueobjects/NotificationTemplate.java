package com.takodu.api.conversations.domain.model.valueobjects;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * W11-W13: the utility templates Takodu creates on each business's WhatsApp account, in Spanish. A body never starts
 * or ends with a parameter, as Meta requires; the same text, filled in, is the free-form message when the window is
 * open and what the inbox shows.
 */
public enum NotificationTemplate {
    REMINDER("takodu_reminder",
            "Hola {{1}}, te recordamos tu cita de {{2}} en {{3}} el {{4}} a las {{5}}. "
                    + "Si necesitas cambiarla, responde a este mensaje.",
            List.of("Confirmar", "Reprogramar"),
            List.of("Ana", "Corte", "Sede Miraflores", "sábado 24 de octubre", "10:00")),
    CONFIRMATION("takodu_confirmation",
            "Hola {{1}}, tu cita de {{2}} en {{3}} quedó agendada para el {{4}} a las {{5}}. ¡Te esperamos!",
            List.of("Reprogramar"),
            List.of("Ana", "Corte", "Sede Miraflores", "sábado 24 de octubre", "10:00")),
    CANCELLATION("takodu_cancellation",
            "Hola {{1}}, tu cita de {{2}} del {{3}} fue cancelada: {{4}}. Escríbenos para reprogramar.",
            List.of(),
            List.of("Ana", "Corte", "sábado 24 de octubre a las 10:00", "el local cerrará por mantenimiento"));

    /** Meta's code for Spanish. */
    public static final String LANGUAGE = "es";
    private static final Pattern PARAMETER = Pattern.compile("\\{\\{(\\d+)}}");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private final String metaName;
    private final String body;
    private final List<String> buttons;
    private final List<String> example;

    NotificationTemplate(String metaName, String body, List<String> buttons, List<String> example) {
        this.metaName = metaName;
        this.body = body;
        this.buttons = buttons;
        this.example = example;
    }

    /** @return the template's name on the WhatsApp account, such as {@code takodu_reminder} */
    public String metaName() {
        return metaName;
    }

    /** @return the body with {@code {{1}}}-style parameters, as it is submitted to Meta */
    public String body() {
        return body;
    }

    /** @return the quick-reply buttons' titles; a tap arrives as an inbound message */
    public List<String> buttons() {
        return buttons;
    }

    public int parameterCount() {
        return (int) PARAMETER.matcher(body).results().count();
    }

    /** @return the parameters Meta shows the reviewer, in order */
    public List<String> example() {
        return example;
    }

    /**
     * @param parameters one value per parameter, in order; line breaks and runs of spaces become one space, since
     *        Meta refuses them inside parameters
     * @return the parameters ready to send
     * @throws IllegalArgumentException when the count is wrong or a value is blank
     */
    public List<String> parameters(List<String> parameters) {
        if (parameters.size() != parameterCount())
            throw new IllegalArgumentException(metaName + " takes " + parameterCount() + " parameters");
        return parameters.stream().map(value -> {
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Template parameters cannot be blank");
            return SPACES.matcher(value.strip()).replaceAll(" ");
        }).toList();
    }

    /** @return the body as the customer reads it */
    public String render(List<String> parameters) {
        var values = parameters(parameters);
        return PARAMETER.matcher(body).replaceAll(match -> Matcher.quoteReplacement(
                values.get(Integer.parseInt(match.group(1)) - 1)));
    }

    public static Optional<NotificationTemplate> fromMetaName(String metaName) {
        return Arrays.stream(values()).filter(template -> template.metaName.equals(metaName)).findFirst();
    }
}
