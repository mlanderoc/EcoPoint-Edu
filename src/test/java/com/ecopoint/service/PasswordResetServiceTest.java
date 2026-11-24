package com.ecopoint.service;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.PasswordResetToken;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.persistence.PasswordResetTokenRepository;
import com.ecopoint.persistence.UserRepository;
import com.ecopoint.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PasswordResetService} (spec D18, RP-01 to RP-08).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class PasswordResetServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 10, 0);
    private static final String EMAIL = "laura@unicesar.edu.co";
    private static final String CODE = "codigo-de-prueba";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final User student = new User(11L, "Laura", "Pérez", EMAIL, UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    private UserRepository users;
    private PasswordResetTokenRepository tokens;
    private PasswordResetMailer mailer;
    private LoginAttemptService loginAttempts;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tokens = mock(PasswordResetTokenRepository.class);
        mailer = mock(PasswordResetMailer.class);
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        loginAttempts = new LoginAttemptService(Clock.fixed(Instant.parse("2026-11-24T15:00:00Z"), ZONE));
        service = new PasswordResetService(users, tokens, mailer, encoder, loginAttempts, clock);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(student));
        when(users.findById(11L)).thenReturn(Optional.of(student));
        when(tokens.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void cp01_activeAccountReceivesALinkAndOnlyTheHashIsStored() {
        service.requestReset(" " + EMAIL + " ");

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendResetLink(eq(student), code.capture());
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(saved.capture());
        assertTrue(code.getValue().length() >= 43);
        assertNotEquals(code.getValue(), saved.getValue().getCodeHash());
        assertEquals(PasswordResetService.hash(code.getValue()), saved.getValue().getCodeHash());
        assertEquals(NOW.plusMinutes(30), saved.getValue().getExpiresAt());
        verify(tokens).invalidateAll(11L, NOW);
    }

    @Test
    void cp02_unknownBlankOrInactiveEmailSendsNothing() {
        when(users.findByEmail("nadie@unicesar.edu.co")).thenReturn(Optional.empty());
        User inactive = new User(12L, "Ana", "Gil", "ana@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
        inactive.deactivate();
        when(users.findByEmail("ana@unicesar.edu.co")).thenReturn(Optional.of(inactive));

        service.requestReset("nadie@unicesar.edu.co");
        service.requestReset("ana@unicesar.edu.co");
        service.requestReset("  ");
        service.requestReset(null);

        verify(mailer, never()).sendResetLink(any(), anyString());
        verify(tokens, never()).save(any());
    }

    @Test
    void cp07_fourthLinkInAnHourIsNotSent() {
        when(tokens.countCreatedSince(11L, NOW.minusHours(1))).thenReturn(3);

        service.requestReset(EMAIL);

        verify(mailer, never()).sendResetLink(any(), anyString());
        verify(tokens, never()).save(any());
    }

    @Test
    void cp03_validCodeChangesThePasswordAndClearsTheLock() {
        givenToken(PasswordResetToken.issue(11L, PasswordResetService.hash(CODE), NOW.minusMinutes(10)));
        when(tokens.markUsed(5L, NOW)).thenReturn(true);
        for (int i = 0; i < LoginAttemptService.MAX_FAILED_ATTEMPTS; i++) {
            loginAttempts.registerFailure(EMAIL);
        }

        User changed = service.confirmReset(CODE, "NuevaClave2026");

        assertEquals(student, changed);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(users).updatePasswordHash(eq(11L), hash.capture());
        assertTrue(encoder.matches("NuevaClave2026", hash.getValue()));
        verify(tokens).invalidateAll(11L, NOW);
        assertFalse(loginAttempts.isLocked(EMAIL));
    }

    @Test
    void cp04_expiredUsedOrUnknownCodeIsRejected() {
        givenToken(PasswordResetToken.issue(11L, PasswordResetService.hash(CODE), NOW.minusMinutes(31)));
        assertInvalidLink(CODE);

        PasswordResetToken used = PasswordResetToken.issue(11L, PasswordResetService.hash(CODE), NOW.minusMinutes(5));
        used.use(NOW.minusMinutes(1));
        givenToken(used);
        assertInvalidLink(CODE);

        assertInvalidLink("codigo-inventado");
        assertInvalidLink(" ");
        assertInvalidLink(null);
        verify(users, never()).updatePasswordHash(anyLong(), anyString());
    }

    @Test
    void cp05_codeTakenByAnotherRequestAtTheSameTimeIsRejected() {
        givenToken(PasswordResetToken.issue(11L, PasswordResetService.hash(CODE), NOW.minusMinutes(5)));
        when(tokens.markUsed(5L, NOW)).thenReturn(false);

        assertInvalidLink(CODE);
        verify(users, never()).updatePasswordHash(anyLong(), anyString());
    }

    @Test
    void cp08_invalidPasswordKeepsTheCodeUnused() {
        givenToken(PasswordResetToken.issue(11L, PasswordResetService.hash(CODE), NOW.minusMinutes(5)));

        InvalidDataException error = assertThrows(InvalidDataException.class, () -> service.confirmReset(CODE, "corta"));

        assertEquals("contrasena", error.getFieldName());
        verify(tokens, never()).markUsed(anyLong(), any());
        verify(users, never()).updatePasswordHash(anyLong(), anyString());
    }

    @Test
    void rp02_codeOfADeactivatedAccountIsRejected() {
        givenToken(PasswordResetToken.issue(11L, PasswordResetService.hash(CODE), NOW.minusMinutes(5)));
        User inactive = new User(11L, "Laura", "Pérez", EMAIL, UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
        inactive.deactivate();
        when(users.findById(11L)).thenReturn(Optional.of(inactive));

        assertInvalidLink(CODE);
    }

    private void givenToken(PasswordResetToken token) {
        token.setId(5L);
        when(tokens.findByHash(PasswordResetService.hash(CODE))).thenReturn(Optional.of(token));
    }

    private void assertInvalidLink(String code) {
        InvalidDataException error = assertThrows(InvalidDataException.class,
                () -> service.confirmReset(code, "NuevaClave2026"));
        assertEquals(PasswordResetService.INVALID_LINK, error.getMessage());
    }
}
