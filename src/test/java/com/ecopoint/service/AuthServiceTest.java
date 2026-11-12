package com.ecopoint.service;

import com.ecopoint.exceptions.UnauthorizedException;
import com.ecopoint.model.Institution;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.UserRepository;
import com.ecopoint.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Login and current user (spec 003, CA-201, CA-202 and R-126).
 *
 * @author EstefaniaMarquez
 */
class AuthServiceTest {

    private static final String EMAIL = "docente.demo@unicesar.edu.co";
    private static final String PASSWORD = "EcoPoint2026";
    /** Hash of the demo password, the same as in sql/002_seed.sql. */
    private static final String SEED_HASH = "$2a$10$MhdpayjBM9C3p0KGJ8zzwuMd.EZcmQKyTk9hnnaqxv04Vdhwal.Um";

    private UserRepository users;
    private LoginAttemptService loginAttempts;
    private AuthService auth;
    private User teacher;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        InstitutionRepository institutions = mock(InstitutionRepository.class);
        loginAttempts = new LoginAttemptService(Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC));
        auth = new AuthService(users, institutions, new BCryptPasswordEncoder(10), loginAttempts);

        teacher = new User(1L, "Daniela", "Arias", EMAIL, UserRole.DOCENTE, 7L);
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        when(users.findPasswordHashByEmail(EMAIL)).thenReturn(Optional.of(SEED_HASH));
        when(users.findById(1L)).thenReturn(Optional.of(teacher));
        when(institutions.findById(7L)).thenReturn(Optional.of(new Institution(7L, "I.E. Prudencia Daza", "Valledupar")));
    }

    @Test
    void ca201_correctEmailAndPasswordReturnTheUserWithItsInstitution() {
        AuthenticatedUser result = auth.login(EMAIL, PASSWORD);

        assertEquals(teacher, result.user());
        assertEquals("I.E. Prudencia Daza", result.institution().getName());
        assertTrue(result.permissions().contains(Permission.MANAGE_STUDIES));
    }

    @Test
    void ca202_wrongPasswordMissingEmailAndInactiveAccountGetTheSameMessage() {
        assertEquals(AuthService.INVALID_CREDENTIALS, loginError(EMAIL, "otra-clave"));
        assertEquals(AuthService.INVALID_CREDENTIALS, loginError("nadie@unicesar.edu.co", PASSWORD));
        assertEquals(AuthService.INVALID_CREDENTIALS, loginError(null, null));

        teacher.deactivate();
        assertEquals(AuthService.INVALID_CREDENTIALS, loginError(EMAIL, PASSWORD));
    }

    @Test
    void ca202_userWithoutPasswordCannotLogIn() {
        when(users.findPasswordHashByEmail(EMAIL)).thenReturn(Optional.empty());

        assertEquals(AuthService.INVALID_CREDENTIALS, loginError(EMAIL, PASSWORD));
    }

    @Test
    void r126_fiveFailuresLockTheAccountEvenWithTheRightPassword() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILED_ATTEMPTS; i++) {
            loginError(EMAIL, "otra-clave");
        }

        assertEquals(AuthService.ACCOUNT_LOCKED, loginError(EMAIL, PASSWORD));
    }

    @Test
    void r126_successfulLoginResetsTheFailures() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILED_ATTEMPTS - 1; i++) {
            loginError(EMAIL, "otra-clave");
        }
        auth.login(EMAIL, PASSWORD);

        loginError(EMAIL, "otra-clave");

        assertFalse(loginAttempts.isLocked(EMAIL));
    }

    @Test
    void r125_currentUserIsReloadedFromTheDatabase() {
        assertEquals("Daniela", auth.currentUser(1L).user().getFirstName());

        teacher.deactivate();
        assertThrows(UnauthorizedException.class, () -> auth.currentUser(1L));
        assertThrows(UnauthorizedException.class, () -> auth.currentUser(99L));
    }

    private String loginError(String email, String password) {
        return assertThrows(UnauthorizedException.class, () -> auth.login(email, password)).getMessage();
    }
}
