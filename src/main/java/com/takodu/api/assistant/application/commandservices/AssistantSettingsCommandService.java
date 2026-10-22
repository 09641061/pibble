package com.takodu.api.assistant.application.commandservices;

import com.takodu.api.assistant.domain.exceptions.AssistantSettingsChangedException;
import com.takodu.api.assistant.domain.exceptions.EstablishmentArchivedException;
import com.takodu.api.assistant.domain.exceptions.EstablishmentNotFoundException;
import com.takodu.api.assistant.domain.model.aggregates.AssistantSettings;
import com.takodu.api.assistant.domain.model.commands.ConfigureAssistantCommand;
import com.takodu.api.shared.domain.exceptions.AccessDeniedException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;

public interface AssistantSettingsCommandService {

    /**
     * A1: an owner or a manager turns the assistant on or off and writes its business info.
     *
     * @throws AccessDeniedException when the caller is staff
     * @throws EstablishmentNotFoundException when the establishment is not in the organization
     * @throws PlanInactiveException when the plan is inactive (O6)
     * @throws EstablishmentArchivedException when the establishment is archived (O4)
     * @throws AssistantSettingsChangedException when someone else saved the first settings meanwhile
     */
    AssistantSettings handle(ConfigureAssistantCommand command);
}
