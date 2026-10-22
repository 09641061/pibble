package com.takodu.api.conversations.application.internal.jobs;

import com.takodu.api.conversations.application.commandservices.ScheduledMessageCommandService;
import com.takodu.api.conversations.domain.model.commands.DispatchNextDueNotificationCommand;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;

/**
 * W11-W15: turns due confirmations, reminders and cancellations into outbox messages, which
 * {@link OutboundMessageSender} delivers. A confirmation waits at most one run.
 */
@ApplicationScoped
public class ScheduledMessageDispatcher {
    private static final int BATCH = 20;

    private final ScheduledMessageCommandService scheduledMessageCommandService;
    private final Clock clock;

    public ScheduledMessageDispatcher(ScheduledMessageCommandService scheduledMessageCommandService, Clock clock) {
        this.scheduledMessageCommandService = scheduledMessageCommandService;
        this.clock = clock;
    }

    /**
     * Each call handles at most one due notification in its own transaction; stops when none is left, or at the first
     * unexpected error, which rolled its row back to be tried at the next run.
     */
    @Scheduled(every = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void dispatchDue() {
        for (int i = 0; i < BATCH; i++) {
            try {
                if (!scheduledMessageCommandService.handle(new DispatchNextDueNotificationCommand(clock.instant())))
                    return;
            } catch (RuntimeException e) {
                Log.warnf(e, "Scheduled message dispatch failed");
                return;
            }
        }
    }
}
