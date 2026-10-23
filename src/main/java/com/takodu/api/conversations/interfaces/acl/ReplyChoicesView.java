package com.takodu.api.conversations.interfaces.acl;

import java.util.List;

/**
 * The choices under an assistant reply, as the assistant decided to show them (08 §5).
 *
 * @param titles 1 to 3 of at most 20 characters for buttons; 1 to 10 of at most 24 for a list
 */
public record ReplyChoicesView(Style style, List<String> titles) {
    public enum Style { BUTTONS, LIST }
}
