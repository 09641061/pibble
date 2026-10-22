package com.takodu.api.conversations.interfaces.acl;

import java.time.Instant;

/**
 * One message of the chat, for the assistant's memory.
 *
 * @param role {@code customer}, {@code assistant} (its replies and Takodu's notifications) or {@code staff} (staff in
 *        Takodu and the owner on the phone)
 * @param text the text, or the kind in brackets, such as {@code [audio]}, when there is none
 */
public record ChatLineView(String role, String text, Instant at) { }
