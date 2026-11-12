package com.ecopoint.validation;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AccessPolicy} (RN-081).
 *
 * @author craulgomez
 * @version 1.0
 */
class AccessPolicyTest {

    private static User teacher() {
        return new User(1L, "Oriana", "Jurado", "oriana@unicesar.edu.co", UserRole.DOCENTE, 1L);
    }

    private static User student() {
        return new User(2L, "Luis", "Gómez", "luis@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    }

    @Test
    void rn081_teacherMayManageStudies() {
        assertDoesNotThrow(() -> AccessPolicy.require(teacher(), Permission.MANAGE_STUDIES));
    }

    @Test
    void rn081_studentCannotCloseShiftsAndGetsForbiddenWithSpanishMessage() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> AccessPolicy.require(student(), Permission.MANAGE_SHIFTS));

        assertEquals("ERR_FORBIDDEN", exception.getErrorCode());
        assertEquals("El rol Estudiante universitario no tiene permiso para abrir, cerrar y reabrir turnos.",
                exception.getMessage());
    }

    @Test
    void rn056_studentMayRegisterHazardousWaste() {
        assertDoesNotThrow(() -> AccessPolicy.require(student(), Permission.REGISTER_HAZARDOUS_WASTE));
    }

    @Test
    void rn081_inactiveUserIsDeniedEvenWithThePermission() {
        User teacher = teacher();
        teacher.deactivate();

        assertThrows(AccessDeniedException.class, () -> AccessPolicy.require(teacher, Permission.VIEW_INDICATORS));
        assertFalse(AccessPolicy.can(teacher, Permission.VIEW_INDICATORS));
    }

    @Test
    void rn081_missingUserIsDenied() {
        assertThrows(AccessDeniedException.class, () -> AccessPolicy.require(null, Permission.REGISTER_BAGS));
        assertFalse(AccessPolicy.can(null, Permission.REGISTER_BAGS));
    }

    @Test
    void rn081_canAnswersWithoutThrowing() {
        assertTrue(AccessPolicy.can(student(), Permission.ATTEND_ALERTS));
        assertFalse(AccessPolicy.can(student(), Permission.MANAGE_ADMINISTRATION));
    }
}
