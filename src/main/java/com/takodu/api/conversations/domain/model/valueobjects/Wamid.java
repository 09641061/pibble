package com.takodu.api.conversations.domain.model.valueobjects;

/**
 * W4: WhatsApp's id of a message, unique across WhatsApp.
 *
 * @param value such as {@code wamid.HBgLMTY0NjcwNDM1OTUVAgARGBIyNDlBOEI5QUQ4NDc0N0FCNjMA}
 */
public record Wamid(String value) {
    public Wamid {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("wamid is required");
        if (value.length() > 256) throw new IllegalArgumentException("wamid is too long");
    }
}
