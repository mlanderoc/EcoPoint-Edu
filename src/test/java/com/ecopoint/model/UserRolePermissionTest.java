package com.ecopoint.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies every cell of the permission matrix RN-081 (4 roles x 11 actions).
 * Abbreviations in the table: COORD = COORDINADOR, DOC = DOCENTE,
 * UNIV = ESTUDIANTE_UNIVERSITARIO, COLE = ESTUDIANTE_COLEGIO.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class UserRolePermissionTest {

    @ParameterizedTest(name = "{0}: COORD={1} DOC={2} UNIV={3} COLE={4}")
    @CsvSource({
            "MANAGE_ADMINISTRATION,    true, false, false, false",
            "MANAGE_STUDIES,           true, false, false, false",
            "MANAGE_SHIFTS,            true, true,  false, false",
            "REGISTER_POPULATION,      true, true,  true,  false",
            "REGISTER_BAGS,            true, true,  true,  true",
            "REGISTER_QUARTERING,      true, true,  true,  true",
            "REGISTER_HAZARDOUS_WASTE, true, true,  false, false",
            "VERIFY_RECORDS,           true, true,  true,  false",
            "ANNUL_QUARTERING,         true, true,  false, false",
            "ATTEND_ALERTS,            true, true,  false, false",
            "VIEW_INDICATORS,          true, true,  true,  true"
    })
    void rn081_permissionMatrix(Permission permission, boolean coordinator, boolean teacher,
                                boolean universityStudent, boolean schoolStudent) {
        assertEquals(coordinator, UserRole.COORDINADOR.hasPermission(permission));
        assertEquals(teacher, UserRole.DOCENTE.hasPermission(permission));
        assertEquals(universityStudent, UserRole.ESTUDIANTE_UNIVERSITARIO.hasPermission(permission));
        assertEquals(schoolStudent, UserRole.ESTUDIANTE_COLEGIO.hasPermission(permission));
    }

    @Test
    void rn081_matrixCoversEveryPermission() {
        assertEquals(11, Permission.values().length);
    }

    @Test
    void rn081_nullPermissionIsNeverGranted() {
        assertFalse(UserRole.COORDINADOR.hasPermission(null));
    }

    @Test
    void rn064_schoolStudentCannotVerifyNorRegisterHazardousWaste() {
        assertFalse(UserRole.ESTUDIANTE_COLEGIO.hasPermission(Permission.VERIFY_RECORDS));
        assertFalse(UserRole.ESTUDIANTE_COLEGIO.hasPermission(Permission.REGISTER_HAZARDOUS_WASTE));
    }

    @Test
    void rn085_onlyCoordinatorAndTeacherCanSuperviseACrew() {
        assertTrue(UserRole.COORDINADOR.canSuperviseCrew());
        assertTrue(UserRole.DOCENTE.canSuperviseCrew());
        assertFalse(UserRole.ESTUDIANTE_UNIVERSITARIO.canSuperviseCrew());
        assertFalse(UserRole.ESTUDIANTE_COLEGIO.canSuperviseCrew());
    }

    @Test
    void rn081_permissionSetCannotBeModifiedFromOutside() {
        assertThrows(UnsupportedOperationException.class,
                () -> UserRole.ESTUDIANTE_COLEGIO.getPermissions().add(Permission.VERIFY_RECORDS));
    }

    @Test
    void rn080_rolesHaveSpanishNames() {
        assertEquals("Estudiante de colegio", UserRole.ESTUDIANTE_COLEGIO.getDisplayName());
        assertEquals("verificar y corregir registros", Permission.VERIFY_RECORDS.getActionDescription());
    }
}
