package com.ecopoint.service;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Crew;
import com.ecopoint.model.Institution;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.persistence.CrewRepository;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.UserActivity;
import com.ecopoint.persistence.UserActivityRepository;
import com.ecopoint.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProfileService} (spec 003, R-132 and R-133; spec 004,
 * R-350 to R-352).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class ProfileServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final User teacher = new User(10L, "Daniela", "Arias", "daniela@unicesar.edu.co", UserRole.DOCENTE, 1L);
    private final User student = new User(11L, "Laura", "Pérez", "laura@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    private UserRepository users;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        InstitutionRepository institutions = mock(InstitutionRepository.class);
        CrewRepository crews = mock(CrewRepository.class);
        UserActivityRepository activity = mock(UserActivityRepository.class);
        service = new ProfileService(users, institutions, crews, activity, encoder);

        when(institutions.findById(1L)).thenReturn(Optional.of(new Institution(1L, "I.E. Prudencia Daza", "Valledupar")));
        Crew morning = new Crew(1L, "Cuadrilla Mañana", teacher);
        morning.addMember(student);
        Crew afternoon = new Crew(2L, "Cuadrilla Tarde", teacher);
        when(crews.findByInstitution(1L)).thenReturn(List.of(morning, afternoon));
        when(activity.findByUser(11L)).thenReturn(new UserActivity(11L, 1, 3, 0, 1, 2, 1));
        when(users.findPasswordHashByEmail("laura@unicesar.edu.co")).thenReturn(Optional.of(encoder.encode("EcoPoint2026")));
    }

    @Test
    void ca314_profileShowsTheOwnDataCrewsAndTotals() {
        UserProfile profile = service.profile(student);

        assertEquals(student, profile.user());
        assertEquals("I.E. Prudencia Daza", profile.institution().getName());
        assertEquals(List.of("Cuadrilla Mañana"), profile.crews());
        assertEquals(3, profile.activity().bags());
        assertEquals(List.of("Cuadrilla Mañana", "Cuadrilla Tarde"), service.profile(teacher).crews());
    }

    @Test
    void r132_passwordChangesOnlyWithTheRightCurrentPassword() {
        InvalidDataException wrong = assertThrows(InvalidDataException.class,
                () -> service.changePassword(student, "Equivocada1", "NuevaClave2026"));
        assertEquals("contrasenaActual", wrong.getFieldName());
        assertThrows(InvalidDataException.class, () -> service.changePassword(student, "EcoPoint2026", "corta"));
        verify(users, never()).updatePasswordHash(anyLong(), anyString());

        service.changePassword(student, "EcoPoint2026", "NuevaClave2026");
        verify(users).updatePasswordHash(eq(11L), argThat(
                hash -> encoder.matches("NuevaClave2026", hash)));
    }
}
