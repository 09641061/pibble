package com.takodu.api.assistant.application.internal.jobs;

import com.takodu.api.assistant.application.commandservices.AssistantTurnCommandService;
import com.takodu.api.assistant.domain.model.commands.RunNextTurnCommand;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.util.concurrent.Semaphore;

/**
 * Answers queued assistant turns. A customer message wakes it after commit; the schedule picks up anything a lost
 * wake-up, a retry or a crashed worker left behind. At most {@link #PARALLEL} turns run at once, each waiting on
 * the language model on its own virtual thread.
 */
@ApplicationScoped
public class TurnWorker {
    /** takodu-jobs: at most this many turns wait on the model at once. */
    static final int PARALLEL = 4;
    private static final int BATCH = 20;

    private final AssistantTurnCommandService assistantTurnCommandService;
    private final Clock clock;
    private final boolean wakeUps;
    private final Semaphore workers = new Semaphore(PARALLEL);

    /** @param wakeUps false in tests, which call {@link #runDue()} themselves so nothing runs in the background */
    public TurnWorker(AssistantTurnCommandService assistantTurnCommandService, Clock clock,
                      @ConfigProperty(name = "takodu.jobs.wake-ups", defaultValue = "true") boolean wakeUps) {
        this.assistantTurnCommandService = assistantTurnCommandService;
        this.clock = clock;
        this.wakeUps = wakeUps;
    }

    @Scheduled(every = "10s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runInParallel() {
        for (int i = 0; i < PARALLEL; i++) startWorker();
    }

    /** A customer message was stored; a free worker starts at once. */
    public void wake() {
        if (wakeUps) startWorker();
    }

    /**
     * Each call runs at most one due turn; stops when none is left, or at the first unexpected error, whose claim
     * rolled back to be tried at the next run.
     */
    public void runDue() {
        for (int i = 0; i < BATCH; i++) {
            try {
                if (!assistantTurnCommandService.handle(new RunNextTurnCommand(clock.instant()))) return;
            } catch (RuntimeException e) {
                Log.warnf(e, "Assistant turn run failed");
                return;
            }
        }
    }

    private void startWorker() {
        if (!workers.tryAcquire()) return;
        Thread.ofVirtual().start(() -> {
            try {
                runDue();
            } finally {
                workers.release();
            }
        });
    }
}
