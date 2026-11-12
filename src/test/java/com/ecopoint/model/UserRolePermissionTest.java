package com.ecopoint.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies every cell of the permission matrix RN-081 (2 roles x 11 actions).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class UserRolePermissionTest {

    @ParameterizedTest(name = "{0}: DOCENTE={1} ESTUDIANTE_UNIVERSITARIO={2}")
    @CsvSource({
            "MANAGE_ADMINISTRATION,    true, false",
            "MANAGE_STUDIES,           true, false",
            "MANAGE_SHIFTS,            true, false",
            "REGISTER_POPULATION,      true, true",
            "REGISTER_BAGS,            true, true",
            "REGISTER_QUARTERING,      true, true",
            "REGISTER_HAZARDOUS_WASTE, true, true",
            "VERIFY_RECORDS,           true, true",
            "ANNUL_QUARTERING,         true, true",
            "ATTEND_ALERTS,            true, true",
            "VIEW_INDICATORS,          true, true"
    })
    void rn081_permissionMatrix(Permission permission, boolean teacher, boolean universityStudent) {
        assertEquals(teacher, UserRole.DOCENTE.hasPermission(permission));
        assertEquals(universityStudent, UserRole.ESTUDIANTE_UNIVERSITARIO.hasPermission(permission));
    }

    @Test
    void rn080_thereAreExactlyTwoRoles() {
        assertEquals(2, UserRole.values().length);
    }

    @Test
    void rn081_matrixCoversEveryPermission() {
        assertEquals(11, Permission.values().length);
    }

    @Test
    void rn081_teacherHasEveryPermission() {
        for (Permission permission : Permission.values()) {
            assertTrue(UserRole.DOCENTE.hasPermission(permission), permission.name());
        }
    }

    @Test
    void rn081_nullPermissionIsNeverGranted() {
        assertFalse(UserRole.DOCENTE.hasPermission(null));
    }

    @Test
    void rn085_onlyTeacherCanSuperviseACrew() {
        assertTrue(UserRole.DOCENTE.canSuperviseCrew());
        assertFalse(UserRole.ESTUDIANTE_UNIVERSITARIO.canSuperviseCrew());
    }

    @Test
    void rn081_permissionSetCannotBeModifiedFromOutside() {
        assertThrows(UnsupportedOperationException.class,
                () -> UserRole.ESTUDIANTE_UNIVERSITARIO.getPermissions().add(Permission.MANAGE_STUDIES));
    }

    @Test
    void rn080_rolesHaveSpanishNames() {
        assertEquals("Estudiante universitario", UserRole.ESTUDIANTE_UNIVERSITARIO.getDisplayName());
        assertEquals("abrir, cerrar y reabrir turnos", Permission.MANAGE_SHIFTS.getActionDescription());
    }
}
