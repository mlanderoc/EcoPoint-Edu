package com.ecopoint.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks an email for 15 minutes after 5 failed logins in a row (spec 003, R-126).
 * The count lives in memory, so it starts again if the server restarts
 * (decision T6).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Service
public class LoginAttemptService {

    /** Failed logins in a row that lock the account. */
    public static final int MAX_FAILED_ATTEMPTS = 5;
    /** Time the account stays locked. */
    public static final Duration LOCK_TIME = Duration.ofMinutes(15);

    private final Clock clock;
    private final Map<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    /**
     * Creates the service.
     *
     * @param clock clock used to measure the lock time
     */
    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Tells whether the email is locked right now.
     *
     * @param email email used to log in
     * @return true while the lock lasts
     */
    public boolean isLocked(String email) {
        Attempts attempts = attemptsByEmail.get(key(email));
        return attempts != null && attempts.lockedUntil() != null && clock.instant().isBefore(attempts.lockedUntil());
    }

    /**
     * Records a failed login; the fifth one in a row locks the email.
     *
     * @param email email used to log in
     */
    public void registerFailure(String email) {
        attemptsByEmail.compute(key(email), (ignored, previous) -> {
            int failures = previous == null || previous.lockExpired(clock.instant()) ? 1 : previous.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILED_ATTEMPTS ? clock.instant().plus(LOCK_TIME) : null;
            return new Attempts(failures, lockedUntil);
        });
    }

    /**
     * Records a successful login, which resets the count.
     *
     * @param email email used to log in
     */
    public void registerSuccess(String email) {
        attemptsByEmail.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private record Attempts(int failures, Instant lockedUntil) {

        boolean lockExpired(Instant now) {
            return lockedUntil != null && !now.isBefore(lockedUntil);
        }
    }
}
