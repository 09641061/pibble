package com.takodu.api.iam.application.internal.jobs;

import com.takodu.api.iam.domain.repositories.LoginCodeRepository;
import com.takodu.api.iam.domain.repositories.SessionRepository;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Deletes sign-in codes and sessions that can no longer be used. Rows are kept one day after they end, which
 * covers the one-hour window of the code rate limit (I3).
 */
@ApplicationScoped
public class CleanupJob {
    static final Duration RETENTION = Duration.ofDays(1);

    private final LoginCodeRepository loginCodeRepository;
    private final SessionRepository sessionRepository;
    private final Clock clock;

    public CleanupJob(LoginCodeRepository loginCodeRepository, SessionRepository sessionRepository, Clock clock) {
        this.loginCodeRepository = loginCodeRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    /** Daily at 04:30 UTC (23:30 in Lima), when nobody signs in. */
    @Scheduled(cron = "0 30 4 * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void deleteEnded() {
        var cutoff = clock.instant().minus(RETENTION);
        long codes = loginCodeRepository.deleteExpiredBefore(cutoff);
        long sessions = sessionRepository.deleteEndedBefore(cutoff);
        Log.infof("Deleted %d sign-in codes and %d sessions that ended before %s", codes, sessions, cutoff);
    }
}
