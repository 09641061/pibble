package com.takodu.api.assistant.application.internal.commandservices;

import com.takodu.api.assistant.application.commandservices.AssistantSettingsCommandService;
import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.assistant.domain.exceptions.EstablishmentArchivedException;
import com.takodu.api.assistant.domain.model.aggregates.AssistantSettings;
import com.takodu.api.assistant.domain.model.commands.ConfigureAssistantCommand;
import com.takodu.api.assistant.domain.repositories.AssistantSettingsRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import com.takodu.api.shared.application.tenancy.Action;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AssistantSettingsCommandServiceImpl implements AssistantSettingsCommandService {
    private final AssistantSettingsRepository assistantSettingsRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final AccessPolicy accessPolicy;

    public AssistantSettingsCommandServiceImpl(AssistantSettingsRepository assistantSettingsRepository,
                                               ExternalOrganizationService externalOrganizationService,
                                               AccessPolicy accessPolicy) {
        this.assistantSettingsRepository = assistantSettingsRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.accessPolicy = accessPolicy;
    }

    @Override
    @Transactional
    public AssistantSettings handle(ConfigureAssistantCommand command) {
        accessPolicy.require(Action.MANAGE_ASSISTANT);
        var establishment = externalOrganizationService.fetchEstablishment(command.organizationId(),
                command.establishmentId());
        if (!establishment.planActive()) throw new PlanInactiveException(command.organizationId().value());
        if (establishment.archived()) throw new EstablishmentArchivedException();
        var settings = assistantSettingsRepository.findByEstablishment(command.organizationId(),
                        command.establishmentId())
                .orElseGet(() -> AssistantSettings.defaultsFor(command.organizationId(), command.establishmentId()));
        settings.configure(command.enabled(), command.businessInfo());
        return assistantSettingsRepository.save(settings);
    }
}
