package com.takodu.api.assistant.domain.model.valueobjects;

import java.util.List;
import java.util.stream.IntStream;

/**
 * A8: what the assistant answers in one message, and the choices it offers the customer.
 *
 * @param options short choices, such as times or services, without repeats; empty when there are none
 */
public record AssistantReply(String text, List<String> options) {
    /** 08 §7: a reply is at most this long. */
    public static final int MAX_TEXT = 1000;
    public static final int MAX_BUTTONS = 3;
    public static final int MAX_BUTTON_TITLE = 20;
    public static final int MAX_ROWS = 10;
    public static final int MAX_ROW_TITLE = 24;
    /** The text above buttons or a list when the model wrote none; WhatsApp requires one. */
    static final String CHOOSE = "Elige una opción:";

    public AssistantReply {
        text = text == null ? "" : text.strip();
        options = options == null ? List.of() : options.stream()
                .filter(option -> option != null && !option.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
        if (text.isEmpty() && options.isEmpty()) throw new IllegalArgumentException("The assistant's reply is empty");
    }

    /** 08 §5: buttons when up to 3 short options fit, a list for up to 10, numbered lines otherwise. */
    public ReplyFormat format() {
        if (options.isEmpty()) return ReplyFormat.TEXT;
        if (fits(MAX_BUTTONS, MAX_BUTTON_TITLE)) return ReplyFormat.BUTTONS;
        if (fits(MAX_ROWS, MAX_ROW_TITLE)) return ReplyFormat.LIST;
        return ReplyFormat.TEXT;
    }

    /**
     * The message text: for {@link ReplyFormat#TEXT}, the text with any options as numbered lines; otherwise the text
     * above the choices. Either way it is cut to 1,000 characters.
     */
    public String body() {
        if (format() != ReplyFormat.TEXT) return cut(text.isEmpty() ? CHOOSE : text);
        var numbered = IntStream.range(0, options.size())
                .mapToObj(i -> (i + 1) + ". " + options.get(i))
                .toList();
        return cut(numbered.isEmpty() ? text : (text.isEmpty() ? "" : text + "\n") + String.join("\n", numbered));
    }

    private boolean fits(int max, int maxTitle) {
        return options.size() <= max && options.stream().allMatch(option -> option.length() <= maxTitle);
    }

    private static String cut(String all) {
        return all.length() <= MAX_TEXT ? all : all.substring(0, MAX_TEXT - 1).stripTrailing() + "…";
    }
}
