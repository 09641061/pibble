package com.takodu.api.assistant.application.internal.commandservices;

import com.takodu.api.assistant.application.commandservices.AssistantTurnCommandService;
import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalConversationService;
import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.assistant.application.internal.outboundservices.llm.AssistantModel;
import com.takodu.api.assistant.application.internal.outboundservices.llm.TurnPrompt;
import com.takodu.api.assistant.application.internal.tools.TurnScope;
import com.takodu.api.assistant.domain.model.aggregates.AssistantTurn;
import com.takodu.api.assistant.domain.model.commands.EnqueueAssistantTurnCommand;
import com.takodu.api.assistant.domain.model.commands.RunNextTurnCommand;
import com.takodu.api.assistant.domain.model.valueobjects.AssistantReply;
import com.takodu.api.assistant.domain.model.valueobjects.ChatLine;
import com.takodu.api.assistant.domain.model.valueobjects.EstablishmentProfile;
import com.takodu.api.assistant.domain.model.valueobjects.TurnStatus;
import com.takodu.api.assistant.domain.model.valueobjects.TurnUsage;
import com.takodu.api.assistant.domain.repositories.AssistantSettingsRepository;
import com.takodu.api.assistant.domain.repositories.AssistantTurnRepository;
import com.takodu.api.assistant.domain.services.AnswerPolicy;
import com.takodu.api.assistant.domain.services.TurnKeys;
import com.takodu.api.shared.application.tenancy.TenantContext;
import io.quarkus.arc.Arc;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ApplicationScoped
public class AssistantTurnCommandServiceImpl implements AssistantTurnCommandService {
    /** 08 §1: the model remembers this many messages of the chat. */
    static final int MEMORY_MESSAGES = 20;
    /** A7: what the customer reads when they send something the assistant cannot read. */
    static final String UNREADABLE_REPLY =
            "Por ahora solo puedo leer mensajes de texto. Una persona del equipo continuará la conversación.";
    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern(
            "EEEE d 'de' MMMM 'de' yyyy', son las' HH:mm", Locale.forLanguageTag("es-PE"));

    private final AssistantTurnRepository assistantTurnRepository;
    private final AssistantSettingsRepository assistantSettingsRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final ExternalConversationService externalConversationService;
    private final AssistantModel assistantModel;
    private final TurnScope turnScope;
    private final TenantContext tenantContext;
    private final Clock clock;

    public AssistantTurnCommandServiceImpl(AssistantTurnRepository assistantTurnRepository,
                                           AssistantSettingsRepository assistantSettingsRepository,
                                           ExternalOrganizationService externalOrganizationService,
                                           ExternalConversationService externalConversationService,
                                           AssistantModel assistantModel, TurnScope turnScope,
                                           TenantContext tenantContext, Clock clock) {
        this.assistantTurnRepository = assistantTurnRepository;
        this.assistantSettingsRepository = assistantSettingsRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.externalConversationService = externalConversationService;
        this.assistantModel = assistantModel;
        this.turnScope = turnScope;
        this.tenantContext = tenantContext;
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

    @Override
    public boolean handle(RunNextTurnCommand command) {
        var claimed = QuarkusTransaction.requiringNew().call(() -> claim(command.now()));
        if (claimed.isEmpty()) return false;
        if (claimed.get().turn().getStatus() == TurnStatus.RUNNING) inOwnRequestContext(() -> run(claimed.get()));
        return true;
    }

    /** A turn that may run, with what its tools need to know. */
    private record Claim(AssistantTurn turn, EstablishmentProfile establishment, String businessInfo) { }

    /**
     * Locks the next due turn and decides, in one transaction, whether it runs: a newer message, an earlier reply,
     * A2 or the daily limit may skip it first.
     */
    private Optional<Claim> claim(Instant now) {
        var found = assistantTurnRepository.claimNext(now);
        if (found.isEmpty()) return Optional.empty();
        var turn = found.get();
        if (assistantTurnRepository.hasNewerPending(turn))
            return skipped(turn, "The customer wrote again; that turn answers both", now);
        var answeredThrough = assistantTurnRepository.answeredThrough(turn.getOrganizationId(),
                turn.getConversationId());
        if (turn.isAnsweredBy(answeredThrough.orElse(null)))
            return skipped(turn, "An earlier reply already answered this message", now);
        var settings = assistantSettingsRepository.findByEstablishment(turn.getOrganizationId(),
                turn.getEstablishmentId());
        var establishment = externalOrganizationService.fetchEstablishment(turn.getOrganizationId(),
                turn.getEstablishmentId());
        var conversation = externalConversationService.fetchConversation(turn.getOrganizationId(),
                turn.getConversationId());
        if (conversation.isEmpty()) return skipped(turn, "The conversation no longer exists", now);
        var quiet = AnswerPolicy.whyQuiet(settings.orElse(null), establishment, conversation.get());
        if (quiet.isPresent()) return skipped(turn, quiet.get(), now);
        if (assistantTurnRepository.countAnsweredSince(turn.getOrganizationId(), turn.getConversationId(),
                now.minus(Duration.ofDays(1))) >= AnswerPolicy.MAX_TURNS_PER_DAY) {
            externalConversationService.handOff(turn.getOrganizationId(), turn.getConversationId(),
                    "El asistente llegó a su límite de respuestas del día en esta conversación");
            return skipped(turn, "The chat reached the assistant's daily limit", now);
        }
        turn.begin(now);
        return Optional.of(new Claim(assistantTurnRepository.save(turn), establishment,
                settings.orElseThrow().getBusinessInfo()));
    }

    private Optional<Claim> skipped(AssistantTurn turn, String reason, Instant now) {
        turn.skip(reason, now);
        return Optional.of(new Claim(assistantTurnRepository.save(turn), null, null));
    }

    /**
     * The model and its tools run outside any transaction: each booking write commits or fails on its own, so a
     * refused booking never spoils the turn. The reply and the handoff go through conversations, which drops the
     * reply if a person wrote meanwhile.
     */
    private void run(Claim claim) {
        var turn = claim.turn();
        tenantContext.bindAssistant(turn.getOrganizationId(), turn.getEstablishmentId());
        turnScope.bind(turn, claim.establishment(), claim.businessInfo());
        TurnUsage usage = null;
        int toolCalls = 0;
        try {
            AssistantReply reply;
            Instant answeredThrough;
            if (turn.triggerIsReadable()) {
                var history = externalConversationService.fetchRecentMessages(turn.getOrganizationId(),
                        turn.getConversationId(), MEMORY_MESSAGES);
                var latest = latestFromCustomer(history);
                answeredThrough = latest.isEmpty() ? turn.getTriggerAt() : latest.getLast().at();
                var answer = assistantModel.answer(new TurnPrompt(claim.establishment().name(),
                        TODAY.format(clock.instant().atZone(claim.establishment().zone())), claim.businessInfo(),
                        history.subList(0, history.size() - latest.size()),
                        latest.stream().map(ChatLine::text).collect(Collectors.joining("\n"))));
                usage = answer.usage();
                toolCalls = answer.toolCalls();
                reply = answer.reply();
            } else {
                turnScope.requestHandOff("El cliente envió un mensaje de tipo " + turn.getTriggerKind()
                        + " que el asistente no puede leer");
                reply = new AssistantReply(UNREADABLE_REPLY, List.of());
                answeredThrough = turn.getTriggerAt();
            }
            var sent = externalConversationService.sendReply(turn.getOrganizationId(), turn.getConversationId(),
                    reply, TurnKeys.of(turn.getId(), "reply"));
            if (sent) turnScope.handOffSummary().ifPresent(summary -> externalConversationService.handOff(
                    turn.getOrganizationId(), turn.getConversationId(), summary));
            var finalUsage = usage;
            var finalToolCalls = toolCalls;
            finish(turn, running -> {
                if (sent) running.complete(finalUsage, finalToolCalls, answeredThrough, clock.instant());
                else running.drop("A person took the chat before the reply went out", finalUsage, finalToolCalls,
                        clock.instant());
            });
        } catch (RuntimeException e) {
            Log.warnf("Assistant turn %s failed: %s", turn.getId().value(), e.getClass().getSimpleName());
            var finalUsage = usage;
            var failed = finish(turn, running -> running.fail(e.getClass().getSimpleName() + ": " + e.getMessage(),
                    finalUsage, turnScope.toolCalls(), clock.instant()));
            if (failed.isFailed()) externalConversationService.handOff(turn.getOrganizationId(),
                    turn.getConversationId(), "El asistente no pudo responder después de 3 intentos");
        }
    }

    /** @return the customer's messages after the last message of anyone else: what the turn answers */
    private static List<ChatLine> latestFromCustomer(List<ChatLine> history) {
        int start = history.size();
        while (start > 0 && history.get(start - 1).role() == ChatLine.ChatRole.CUSTOMER) start--;
        return history.subList(start, history.size());
    }

    private AssistantTurn finish(AssistantTurn turn, Consumer<AssistantTurn> outcome) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var locked = assistantTurnRepository.lockById(turn.getOrganizationId(), turn.getId()).orElseThrow();
            outcome.accept(locked);
            return assistantTurnRepository.save(locked);
        });
    }

    /**
     * Each turn gets its own request context, so {@link TenantContext} and {@link TurnScope} hold this turn only,
     * even when the scheduler already opened one for the job.
     */
    private static void inOwnRequestContext(Runnable work) {
        var requestContext = Arc.container().requestContext();
        var outer = requestContext.isActive() ? requestContext.getState() : null;
        if (outer != null) requestContext.deactivate();
        requestContext.activate();
        try {
            work.run();
        } finally {
            requestContext.terminate();
            if (outer != null) requestContext.activate(outer);
        }
    }
}
