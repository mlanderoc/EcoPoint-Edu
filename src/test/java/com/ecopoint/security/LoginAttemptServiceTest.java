package com.ecopoint.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lock after failed logins (spec 003, R-126).
 *
 * @author mlanderoc
 */
class LoginAttemptServiceTest {

    private static final String EMAIL = "docente.demo@unicesar.edu.co";

    private MovableClock clock;
    private LoginAttemptService attempts;

    @BeforeEach
    void setUp() {
        clock = new MovableClock(Instant.parse("2026-11-12T12:00:00Z"));
        attempts = new LoginAttemptService(clock);
    }

    @Test
    void r126_fourFailuresDoNotLockTheAccount() {
        fail(4);

        assertFalse(attempts.isLocked(EMAIL));
    }

    @Test
    void r126_fifthFailureInARowLocksTheAccountIgnoringCase() {
        fail(5);

        assertTrue(attempts.isLocked(EMAIL));
        assertTrue(attempts.isLocked(" DOCENTE.Demo@unicesar.edu.co "));
        assertFalse(attempts.isLocked("estudiante.demo@unicesar.edu.co"));
    }

    @Test
    void r126_lockEndsAfterFifteenMinutes() {
        fail(5);

        clock.advance(Duration.ofMinutes(14).plusSeconds(59));
        assertTrue(attempts.isLocked(EMAIL));

        clock.advance(Duration.ofSeconds(1));
        assertFalse(attempts.isLocked(EMAIL));
    }

    @Test
    void r126_afterTheLockEndsTheCountStartsAgain() {
        fail(5);
        clock.advance(LoginAttemptService.LOCK_TIME);

        fail(4);

        assertFalse(attempts.isLocked(EMAIL));
    }

    @Test
    void r126_successfulLoginResetsTheCount() {
        fail(4);
        attempts.registerSuccess(EMAIL);

        fail(4);

        assertFalse(attempts.isLocked(EMAIL));
    }

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            attempts.registerFailure(EMAIL);
        }
    }

    /** Clock that the test moves forward by hand. */
    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
