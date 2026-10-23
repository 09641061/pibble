package com.takodu.api.conversations.interfaces.acl;

import java.time.Instant;

/**
 * One message of the chat, for the assistant's memory.
 *
 * @param role {@code customer}, {@code assistant} (its replies and Takodu's notifications) or {@code staff} (staff in
 *        Takodu and the owner on the phone)
 * @param text the text, or the kind in brackets, such as {@code [audio]}, when there is none; an assistant reply
 *        with buttons or a list ends with its choices as numbered lines
 */
public record ChatLineView(String role, String text, Instant at) { }
