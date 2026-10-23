package com.takodu.api.assistant.application.internal.queryservices;

import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.assistant.application.queryservices.AssistantSettingsQueryService;
import com.takodu.api.assistant.domain.model.aggregates.AssistantSettings;
import com.takodu.api.assistant.domain.model.queries.GetAssistantSettingsQuery;
import com.takodu.api.assistant.domain.repositories.AssistantSettingsRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AssistantSettingsQueryServiceImpl implements AssistantSettingsQueryService {
    private final AssistantSettingsRepository assistantSettingsRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final AccessPolicy accessPolicy;

    public AssistantSettingsQueryServiceImpl(AssistantSettingsRepository assistantSettingsRepository,
                                             ExternalOrganizationService externalOrganizationService,
                                             AccessPolicy accessPolicy) {
        this.assistantSettingsRepository = assistantSettingsRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public AssistantSettings handle(GetAssistantSettingsQuery query) {
        accessPolicy.require(Action.MANAGE_ASSISTANT);
        externalOrganizationService.fetchEstablishment(query.organizationId(), query.establishmentId());
        return assistantSettingsRepository.findByEstablishment(query.organizationId(), query.establishmentId())
                .orElseGet(() -> AssistantSettings.defaultsFor(query.organizationId(), query.establishmentId()));
    }
}
