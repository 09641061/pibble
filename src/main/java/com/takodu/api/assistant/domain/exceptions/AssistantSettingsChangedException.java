package com.takodu.api.assistant.domain.exceptions;

import com.takodu.api.shared.domain.exceptions.ConflictException;

/** Two people saved the first settings of an establishment at once; the loser reloads and tries again. */
public class AssistantSettingsChangedException extends ConflictException {
    public AssistantSettingsChangedException() {
        super("Assistant settings", "Someone else saved the assistant settings meanwhile; reload them");
    }
}
