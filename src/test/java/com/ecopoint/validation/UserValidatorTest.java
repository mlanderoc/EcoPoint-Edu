package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link UserValidator} (RN-080, RN-084, CA-080).
 *
 * @author craulgomez
 * @version 1.0
 */
class UserValidatorTest {

    private static User student(String email) {
        return new User(null, "Ana María", "Pérez", email, UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    }

    @Test
    void rn080_validUserIsAccepted() {
        assertDoesNotThrow(() -> UserValidator.validate(student("ana.perez@unicesar.edu.co")));
    }

    @Test
    void ca080_emailAlreadyRegisteredIsRejected() {
        User user = student("ana.perez@unicesar.edu.co");

        assertThrows(BusinessRuleException.class, () -> UserValidator.validateUniqueEmail(
                user, Set.of("ana.perez@unicesar.edu.co", "luis@unicesar.edu.co")));
    }

    @Test
    void rn084_emailComparisonIgnoresCaseAndSpaces() {
        User user = student("  Ana.Perez@UNICESAR.edu.co ");

        assertThrows(BusinessRuleException.class,
                () -> UserValidator.validateUniqueEmail(user, Set.of("ana.perez@unicesar.edu.co")));
    }

    @Test
    void rn084_newEmailIsAccepted() {
        assertDoesNotThrow(() -> UserValidator.validateUniqueEmail(
                student("nuevo@unicesar.edu.co"), Set.of("ana.perez@unicesar.edu.co")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ana", "ana@", "ana@unicesar", "ana perez@unicesar.edu.co"})
    void rn084_invalidEmailFormatIsRejected(String email) {
        assertThrows(InvalidDataException.class, () -> UserValidator.validate(student(email)));
    }

    @Test
    void rn080_namesMustBeLettersOnly() {
        User user = new User(null, "Ana2", "Pérez", "ana@unicesar.edu.co", UserRole.DOCENTE, 1L);

        InvalidDataException exception = assertThrows(InvalidDataException.class, () -> UserValidator.validate(user));
        assertEquals("nombre", exception.getFieldName());
    }

    @Test
    void rn080_roleAndInstitutionAreRequired() {
        assertThrows(InvalidDataException.class, () -> UserValidator.validate(
                new User(null, "Ana", "Pérez", "ana@unicesar.edu.co", null, 1L)));
        assertThrows(InvalidDataException.class, () -> UserValidator.validate(
                new User(null, "Ana", "Pérez", "ana@unicesar.edu.co", UserRole.DOCENTE, null)));
    }
}
