package com.takodu.api.conversations.application.internal.jobs;

import com.takodu.api.conversations.application.commandservices.MediaCommandService;
import com.takodu.api.conversations.domain.model.commands.RemoveNextMediaCommand;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.Duration;

/**
 * C5 and 07 §4: deletes the media files of deleted customers, and every file older than
 * {@code takodu.media.retention-days}. Nothing waits on it, so it runs on its schedule only.
 */
@ApplicationScoped
public class MediaRetentionJob {
    private static final int BATCH = 100;

    private final MediaCommandService mediaCommandService;
    private final Clock clock;
    private final Duration retention;

    public MediaRetentionJob(MediaCommandService mediaCommandService, Clock clock,
                             @ConfigProperty(name = "takodu.media.retention-days") int retentionDays) {
        this.mediaCommandService = mediaCommandService;
        this.clock = clock;
        this.retention = Duration.ofDays(retentionDays);
    }

    /**
     * Each call deletes at most one file in its own transaction; stops when none is due, or at the first unexpected
     * error, which rolled its row back to be tried at the next run.
     */
    @Scheduled(every = "10m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void removeDue() {
        for (int i = 0; i < BATCH; i++) {
            try {
                if (!mediaCommandService.handle(new RemoveNextMediaCommand(clock.instant().minus(retention)))) return;
            } catch (RuntimeException e) {
                Log.warnf(e, "Media removal failed");
                return;
            }
        }
    }
}
