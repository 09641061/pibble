package com.takodu.api.assistant.domain.model.valueobjects;

import java.util.List;
import java.util.stream.IntStream;

/**
 * A8: what the assistant answers in one message, and the choices it offers the customer.
 *
 * @param options short choices, such as times or services; empty when there are none
 */
public record AssistantReply(String text, List<String> options) {
    /** 08 §7: a reply is at most this long. */
    public static final int MAX_TEXT = 1000;

    public AssistantReply {
        text = text == null ? "" : text.strip();
        options = options == null ? List.of() : options.stream()
                .filter(option -> option != null && !option.isBlank())
                .map(String::strip)
                .limit(10)
                .toList();
        if (text.isEmpty() && options.isEmpty()) throw new IllegalArgumentException("The assistant's reply is empty");
    }

    /**
     * 08 §5, as plain text: the options become numbered lines under the text. The whole message is cut to 1,000
     * characters.
     */
    public String asText() {
        var numbered = IntStream.range(0, options.size())
                .mapToObj(i -> (i + 1) + ". " + options.get(i))
                .toList();
        var all = numbered.isEmpty() ? text : (text.isEmpty() ? "" : text + "\n") + String.join("\n", numbered);
        return all.length() <= MAX_TEXT ? all : all.substring(0, MAX_TEXT - 1).stripTrailing() + "…";
    }
}
