package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PasswordResetToken} (spec D18, RP-03).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class PasswordResetTokenTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 10, 0);
    private static final String HASH = "a".repeat(64);

    @Test
    void rp03_newCodeWorksForThirtyMinutes() {
        PasswordResetToken token = PasswordResetToken.issue(7L, HASH, NOW);

        assertNull(token.getId());
        assertEquals(7L, token.getUserId());
        assertEquals(NOW.plusMinutes(30), token.getExpiresAt());
        assertTrue(token.isUsable(NOW.plusMinutes(29)));
        assertFalse(token.isUsable(NOW.plusMinutes(30)));
    }

    @Test
    void cp04_usedCodeCannotBeUsedAgain() {
        PasswordResetToken token = PasswordResetToken.issue(7L, HASH, NOW);

        token.use(NOW.plusMinutes(5));

        assertEquals(NOW.plusMinutes(5), token.getUsedAt());
        assertFalse(token.isUsable(NOW.plusMinutes(6)));
        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> token.use(NOW.plusMinutes(6)));
        assertEquals("El enlace no es válido o ya venció.", error.getMessage());
    }

    @Test
    void cp04_expiredCodeCannotBeUsed() {
        PasswordResetToken token = PasswordResetToken.issue(7L, HASH, NOW);

        assertThrows(BusinessRuleException.class, () -> token.use(NOW.plusMinutes(31)));
        assertNull(token.getUsedAt());
    }
}
