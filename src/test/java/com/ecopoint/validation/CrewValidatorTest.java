package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Crew;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CrewValidator} and {@link Crew} (RN-085, CA-081).
 *
 * @author craulgomez
 * @version 1.0
 */
class CrewValidatorTest {

    private static final User TEACHER =
            new User(1L, "Oriana", "Jurado", "oriana@unicesar.edu.co", UserRole.DOCENTE, 1L);

    private static User student(long id) {
        return new User(id, "Estudiante", "Prueba", "e" + id + "@unicesar.edu.co",
                UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    }

    @Test
    void rn085_crewLedByATeacherIsAccepted() {
        Crew crew = new Crew(null, "Cuadrilla 1", TEACHER);
        crew.addMember(student(2));
        crew.addMember(student(3));

        assertDoesNotThrow(() -> CrewValidator.validate(crew));
    }

    @Test
    void ca081_crewLedByAStudentIsRejected() {
        Crew crew = new Crew(null, "Cuadrilla 1", student(2));

        assertThrows(BusinessRuleException.class, () -> CrewValidator.validate(crew));
    }

    @Test
    void ca081_crewWithoutResponsibleIsRejected() {
        Crew crew = new Crew(null, "Cuadrilla 1", null);

        assertThrows(BusinessRuleException.class, () -> CrewValidator.validate(crew));
    }

    @Test
    void rn085_crewNameIsRequired() {
        assertThrows(InvalidDataException.class, () -> CrewValidator.validate(new Crew(null, " ", TEACHER)));
    }

    @Test
    void rn085_inactiveMemberIsRejected() {
        Crew crew = new Crew(null, "Cuadrilla 1", TEACHER);
        User inactive = student(4);
        inactive.deactivate();
        crew.addMember(inactive);

        assertThrows(BusinessRuleException.class, () -> CrewValidator.validate(crew));
    }

    @Test
    void rn085_sameMemberIsNotAddedTwice() {
        Crew crew = new Crew(null, "Cuadrilla 1", TEACHER);

        assertTrue(crew.addMember(student(2)));
        assertFalse(crew.addMember(student(2)));
        assertEquals(1, crew.getMembers().size());
        assertTrue(crew.removeMember(student(2)));
        assertEquals(0, crew.getMembers().size());
    }
}
