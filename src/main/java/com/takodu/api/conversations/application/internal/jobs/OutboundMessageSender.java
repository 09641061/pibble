package com.takodu.api.conversations.application.internal.jobs;

import com.takodu.api.conversations.application.commandservices.MessageCommandService;
import com.takodu.api.conversations.domain.model.commands.SendNextPendingMessageCommand;
import com.takodu.api.conversations.domain.model.events.OutboundMessageQueuedEvent;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;

/**
 * Delivers the outbox: staff replies now, assistant replies later. A queued message wakes it after commit; the
 * schedule picks up anything a lost wake-up or a retry left behind.
 */
@ApplicationScoped
public class OutboundMessageSender {
    private static final int BATCH = 20;

    private final MessageCommandService messageCommandService;
    private final Clock clock;
    private final boolean wakeUps;

    /** @param wakeUps false in tests, which call {@link #sendDue()} themselves so nothing runs in the background */
    public OutboundMessageSender(MessageCommandService messageCommandService, Clock clock,
                                 @ConfigProperty(name = "takodu.jobs.wake-ups", defaultValue = "true") boolean wakeUps) {
        this.messageCommandService = messageCommandService;
        this.clock = clock;
        this.wakeUps = wakeUps;
    }

    /**
     * Each call sends at most one due message in its own transaction; stops when none is left, or at the first
     * unexpected error, which rolled its row back to be tried at the next run.
     */
    @Scheduled(every = "10s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void sendDue() {
        for (int i = 0; i < BATCH; i++) {
            try {
                if (!messageCommandService.handle(new SendNextPendingMessageCommand(clock.instant()))) return;
            } catch (RuntimeException e) {
                Log.warnf(e, "Outbound message dispatch failed");
                return;
            }
        }
    }

    void wake(@Observes(during = TransactionPhase.AFTER_SUCCESS) OutboundMessageQueuedEvent event) {
        if (wakeUps) Thread.ofVirtual().start(this::sendDue);
    }
}
