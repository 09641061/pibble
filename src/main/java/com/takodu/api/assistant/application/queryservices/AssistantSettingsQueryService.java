package com.takodu.api.assistant.application.queryservices;

import com.takodu.api.assistant.domain.model.aggregates.AssistantSettings;
import com.takodu.api.assistant.domain.model.queries.GetAssistantSettingsQuery;

public interface AssistantSettingsQueryService {

    /**
     * @return the settings, or the defaults (off, no business info) when nobody configured them yet
     * @throws com.takodu.api.shared.domain.exceptions.AccessDeniedException when the caller is staff
     * @throws com.takodu.api.assistant.domain.exceptions.EstablishmentNotFoundException when the establishment is
     *         not in the organization
     */
    AssistantSettings handle(GetAssistantSettingsQuery query);
}
