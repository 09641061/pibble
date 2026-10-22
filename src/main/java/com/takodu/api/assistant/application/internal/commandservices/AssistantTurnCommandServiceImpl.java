package com.takodu.api.assistant.application.internal.commandservices;

import com.takodu.api.assistant.application.commandservices.AssistantTurnCommandService;
import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalConversationService;
import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.assistant.domain.model.aggregates.AssistantTurn;
import com.takodu.api.assistant.domain.model.commands.EnqueueAssistantTurnCommand;
import com.takodu.api.assistant.domain.repositories.AssistantSettingsRepository;
import com.takodu.api.assistant.domain.repositories.AssistantTurnRepository;
import com.takodu.api.assistant.domain.services.AnswerPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.util.Optional;

@ApplicationScoped
public class AssistantTurnCommandServiceImpl implements AssistantTurnCommandService {
    private final AssistantTurnRepository assistantTurnRepository;
    private final AssistantSettingsRepository assistantSettingsRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final ExternalConversationService externalConversationService;
    private final Clock clock;

    public AssistantTurnCommandServiceImpl(AssistantTurnRepository assistantTurnRepository,
                                           AssistantSettingsRepository assistantSettingsRepository,
                                           ExternalOrganizationService externalOrganizationService,
                                           ExternalConversationService externalConversationService, Clock clock) {
        this.assistantTurnRepository = assistantTurnRepository;
        this.assistantSettingsRepository = assistantSettingsRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.externalConversationService = externalConversationService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Optional<AssistantTurn> handle(EnqueueAssistantTurnCommand command) {
        if ("reaction".equals(command.kind())) return Optional.empty();
        if (assistantTurnRepository.existsByTriggerMessage(command.organizationId(), command.messageId()))
            return Optional.empty();
        var settings = assistantSettingsRepository.findByEstablishment(command.organizationId(),
                command.establishmentId());
        if (settings.isEmpty() || !settings.get().isEnabled()) return Optional.empty();
        var conversation = externalConversationService.fetchConversation(command.organizationId(),
                command.conversationId());
        if (conversation.isEmpty()) return Optional.empty();
        var establishment = externalOrganizationService.fetchEstablishment(command.organizationId(),
                command.establishmentId());
        if (AnswerPolicy.whyQuiet(settings.get(), establishment, conversation.get()).isPresent())
            return Optional.empty();
        return Optional.of(assistantTurnRepository.save(AssistantTurn.enqueue(command.organizationId(),
                command.establishmentId(), command.conversationId(), command.customerId(), command.messageId(),
                command.kind(), command.sentAt(), clock.instant())));
    }
}
