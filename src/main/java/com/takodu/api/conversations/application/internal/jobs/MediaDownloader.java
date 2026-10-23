package com.takodu.api.conversations.application.internal.jobs;

import com.takodu.api.conversations.application.commandservices.MediaCommandService;
import com.takodu.api.conversations.domain.model.commands.DownloadNextMediaCommand;
import com.takodu.api.conversations.domain.model.events.MediaReceivedEvent;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;

/**
 * 07 §4: downloads customers' images, audio, video, documents and stickers to the media directory. A stored message
 * with media wakes it after commit; the schedule picks up retries and anything a lost wake-up left behind.
 */
@ApplicationScoped
public class MediaDownloader {
    private static final int BATCH = 10;

    private final MediaCommandService mediaCommandService;
    private final Clock clock;
    private final boolean wakeUps;

    /** @param wakeUps false in tests, which call {@link #downloadDue()} themselves so nothing runs in the background */
    public MediaDownloader(MediaCommandService mediaCommandService, Clock clock,
                           @ConfigProperty(name = "takodu.jobs.wake-ups", defaultValue = "true") boolean wakeUps) {
        this.mediaCommandService = mediaCommandService;
        this.clock = clock;
        this.wakeUps = wakeUps;
    }

    /**
     * Each call downloads at most one file in its own transaction; stops when none is due, or at the first
     * unexpected error, which rolled its row back to be tried at the next run.
     */
    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void downloadDue() {
        for (int i = 0; i < BATCH; i++) {
            try {
                if (!mediaCommandService.handle(new DownloadNextMediaCommand(clock.instant()))) return;
            } catch (RuntimeException e) {
                Log.warnf(e, "Media download failed");
                return;
            }
        }
    }

    void wake(@Observes(during = TransactionPhase.AFTER_SUCCESS) MediaReceivedEvent event) {
        if (wakeUps) Thread.ofVirtual().start(this::downloadDue);
    }
}
