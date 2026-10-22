package com.takodu.api.conversations.domain.model.valueobjects;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * The choices an outbound interactive message offers, within WhatsApp's limits (07 §5). Which form a reply takes is
 * the sender's decision; this only refuses what Meta would refuse.
 *
 * @param titles in the order shown; a tap comes back as an inbound message with the title as its body
 */
public record InteractiveChoices(Style style, List<String> titles) {
    public static final int MAX_BUTTONS = 3;
    public static final int MAX_BUTTON_TITLE = 20;
    public static final int MAX_ROWS = 10;
    public static final int MAX_ROW_TITLE = 24;
    /** The label of the button that opens a list; Meta allows 20 characters. */
    public static final String LIST_BUTTON = "Ver opciones";

    public enum Style { BUTTONS, LIST }

    /** @throws IllegalArgumentException when WhatsApp would refuse the choices */
    public InteractiveChoices {
        Objects.requireNonNull(style, "style is required");
        titles = List.copyOf(titles);
        var max = style == Style.BUTTONS ? MAX_BUTTONS : MAX_ROWS;
        var maxTitle = style == Style.BUTTONS ? MAX_BUTTON_TITLE : MAX_ROW_TITLE;
        if (titles.isEmpty() || titles.size() > max)
            throw new IllegalArgumentException("Offer between 1 and " + max + " choices");
        if (titles.stream().anyMatch(title -> title.isBlank() || title.length() > maxTitle))
            throw new IllegalArgumentException("A choice has between 1 and " + maxTitle + " characters");
        if (style == Style.BUTTONS && new HashSet<>(titles).size() < titles.size())
            throw new IllegalArgumentException("Button titles must differ");
    }
}
