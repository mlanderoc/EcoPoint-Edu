package com.ecopoint.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link User}.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class UserTest {

    @Test
    void rn080_fullNameHasNoExtraSpaces() {
        assertEquals("Ana Pérez",
                new User(null, " Ana ", "Pérez ", "ana@unicesar.edu.co", UserRole.DOCENTE, 1L).getFullName());
        assertEquals("Ana", new User(null, "Ana", null, "ana@unicesar.edu.co", UserRole.DOCENTE, 1L).getFullName());
        assertEquals("", new User().getFullName());
    }

    @Test
    void rn084_emailIsNormalizedForComparison() {
        assertEquals("ana@unicesar.edu.co", User.normalizeEmail("  Ana@UNICESAR.edu.co "));
        assertEquals("", User.normalizeEmail(null));
    }

    @Test
    void rn081_inactiveUserHasNoPermissions() {
        User user = new User(1L, "Ana", "Pérez", "ana@unicesar.edu.co", UserRole.DOCENTE, 1L);
        assertTrue(user.hasPermission(Permission.MANAGE_STUDIES));

        user.deactivate();

        assertFalse(user.hasPermission(Permission.MANAGE_STUDIES));
    }
}
