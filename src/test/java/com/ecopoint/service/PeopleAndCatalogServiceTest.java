package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Crew;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import com.ecopoint.persistence.CrewRepository;
import com.ecopoint.persistence.UserActivity;
import com.ecopoint.persistence.UserActivityRepository;
import com.ecopoint.persistence.UserRepository;
import com.ecopoint.persistence.WasteCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Users, crews and waste subcategories (R-322 to R-324, RN-041, RN-042, RN-084, RN-085).
 *
 * @author EstefaniaMarquez
 */
class PeopleAndCatalogServiceTest {

    private UserRepository users;
    private UserActivityRepository activity;
    private CrewRepository crews;
    private WasteCategoryRepository categories;
    private UserService userService;
    private CrewService crewService;
    private WasteCategoryService categoryService;
    private final User teacher = new User(10L, "Daniela", "Arias", "daniela@unicesar.edu.co", UserRole.DOCENTE, 1L);
    private final User student = new User(11L, "Laura", "Pérez", "laura@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        activity = mock(UserActivityRepository.class);
        crews = mock(CrewRepository.class);
        categories = mock(WasteCategoryRepository.class);
        userService = new UserService(users, new BCryptPasswordEncoder(4), activity);
        crewService = new CrewService(crews, users);
        categoryService = new WasteCategoryService(categories);

        when(users.findById(10L)).thenReturn(Optional.of(teacher));
        when(users.findById(11L)).thenReturn(Optional.of(student));
        when(users.findEmails()).thenReturn(Set.of("daniela@unicesar.edu.co", "laura@unicesar.edu.co"));
        when(users.findByInstitution(1L)).thenReturn(List.of(teacher, student));
        when(users.save(any())).thenAnswer(call -> {
            User user = call.getArgument(0);
            if (user.getId() == null) {
                user.setId(20L);
            }
            return user;
        });
        when(crews.save(any())).thenAnswer(call -> call.getArgument(0));
        when(categories.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void ca210_newUserGetsItsPasswordOnlyAsABcryptHash() {
        User created = userService.create(teacher,
                new User(null, "Mario", "Gómez", "mario@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, null),
                "Segura2026");

        assertEquals(1L, created.getInstitutionId(), "Sin institución, entra a la del docente");
        verify(users).updatePasswordHash(eq(20L), argThat(hash -> hash.startsWith("$2a$") && !hash.contains("Segura2026")));
    }

    @Test
    void ca080_repeatedEmailIsRejectedIgnoringCase() {
        assertThrows(BusinessRuleException.class, () -> userService.create(teacher,
                new User(null, "Otra", "Persona", " LAURA@unicesar.edu.co", UserRole.DOCENTE, 1L), "Segura2026"));
    }

    @Test
    void r120_shortPasswordIsRejectedBeforeSaving() {
        assertThrows(InvalidDataException.class, () -> userService.create(teacher,
                new User(null, "Mario", "Gómez", "mario@unicesar.edu.co", UserRole.DOCENTE, 1L), "corta"));
        verify(users, never()).save(any());
    }

    @Test
    void ca203_studentCannotManageUsers() {
        assertThrows(AccessDeniedException.class, () -> userService.list(student, null, null, null));
        assertThrows(AccessDeniedException.class, () -> userService.create(student,
                new User(null, "Mario", "Gómez", "mario@unicesar.edu.co", UserRole.DOCENTE, 1L), "Segura2026"));
    }

    @Test
    void r322_usersAreFilteredByRoleAndState() {
        assertEquals(List.of(student), userService.list(teacher, UserRole.ESTUDIANTE_UNIVERSITARIO, null, null));
        assertEquals(2, userService.list(teacher, null, true, null).size());
    }

    @Test
    void r322_updateKeepsTheOwnEmailAndChangesThePasswordOnlyWhenGiven() {
        User changes = new User(null, "Laura", "Pérez Díaz", "laura@unicesar.edu.co",
                UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);

        assertEquals("Pérez Díaz", userService.update(teacher, 11L, changes, null).getLastName());
        verify(users, never()).updatePasswordHash(anyLong(), any());

        userService.update(teacher, 11L, changes, "NuevaClave2026");
        verify(users).updatePasswordHash(eq(11L), any());
    }

    @Test
    void r322_deactivatedUserIsReported() {
        assertFalse(userService.deactivate(teacher, 11L).isActive());
        verify(users).deactivate(11L);
        assertThrows(ResourceNotFoundException.class, () -> userService.deactivate(teacher, 99L));
    }

    @Test
    void ca081_crewNeedsATeacherAsResponsible() {
        assertThrows(BusinessRuleException.class, () -> crewService.create(teacher, "Cuadrilla A", 11L, List.of()));
        assertThrows(BusinessRuleException.class, () -> crewService.create(teacher, "Cuadrilla A", null, List.of(11L)));
    }

    @Test
    void r323_crewIsSavedWithItsMembersOnce() {
        Crew crew = crewService.create(teacher, "Cuadrilla A", 10L, List.of(11L, 11L));

        assertEquals(teacher, crew.getResponsible());
        assertEquals(1, crew.getMembers().size());
        assertTrue(crew.isMember(student));
    }

    @Test
    void r323_unknownMemberIsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> crewService.create(teacher, "Cuadrilla A", 10L, List.of(99L)));
    }

    @Test
    void rn041_subcategoryNameIsUniqueIgnoringCase() {
        when(categories.findNames()).thenReturn(Set.of("Cartón"));

        assertThrows(BusinessRuleException.class, () -> categoryService.create(teacher,
                new WasteCategory(null, " cartón ", WasteGroup.BLANCO, "Cajas")));
    }

    @Test
    void rn042_subcategoryWithResultsCannotChangeItsGroup() {
        when(categories.findById(3L)).thenReturn(Optional.of(new WasteCategory(3L, "Pilas", WasteGroup.SIN_COLOR, "")));
        when(categories.findNames()).thenReturn(Set.of("Pilas"));
        when(categories.hasResults(3L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> categoryService.update(teacher, 3L,
                new WasteCategory(null, "Pilas", WasteGroup.NEGRO, "")));
        assertEquals("Pilas usadas", categoryService.update(teacher, 3L,
                new WasteCategory(null, "Pilas usadas", WasteGroup.SIN_COLOR, "")).getName());
    }

    @Test
    void r324_subcategoriesAreListedByGroup() {
        when(categories.findAll()).thenReturn(List.of(new WasteCategory(1L, "Papel", WasteGroup.BLANCO, ""),
                new WasteCategory(2L, "Pilas", WasteGroup.SIN_COLOR, "")));

        assertEquals(1, categoryService.list(WasteGroup.SIN_COLOR, null).size());
        assertEquals(2, categoryService.list(null, null).size());
    }

    @Test
    void ca313_usersAreSearchedByNameOrEmail() {
        assertEquals(List.of(student), userService.list(teacher, null, null, "  pérez "));
        assertEquals(List.of(teacher), userService.list(teacher, null, null, "DANIELA@"));
        assertEquals(List.of(), userService.list(teacher, null, false, "laura"));
    }

    @Test
    void ca082_teacherIsNeverDeactivatedAndStudentIsActivatedAgain() {
        assertThrows(BusinessRuleException.class, () -> userService.deactivate(teacher, 10L));
        verify(users, never()).deactivate(10L);

        userService.deactivate(teacher, 11L);
        verify(users).deactivate(11L);
        User activated = userService.activate(teacher, 11L);
        assertTrue(activated.isActive());
        assertThrows(AccessDeniedException.class, () -> userService.activate(student, 11L));
    }

    @Test
    void r322_usersTableGetsTheTotalsOfEveryUser() {
        when(activity.findAll()).thenReturn(java.util.Map.of(11L, new UserActivity(11L, 1, 3, 0, 0, 2, 1)));

        assertEquals(3, userService.activities(teacher).get(11L).bags());
        assertThrows(AccessDeniedException.class, () -> userService.activities(student));
    }

    @Test
    void ca057_subcategoryIsDeactivatedAndActivatedAgain() {
        WasteCategory icopor = new WasteCategory(5L, "Icopor", WasteGroup.NEGRO, null);
        when(categories.findById(5L)).thenReturn(Optional.of(icopor));
        when(categories.save(any())).thenAnswer(call -> call.getArgument(0));
        when(categories.findAll()).thenReturn(List.of(icopor));

        assertFalse(categoryService.deactivate(teacher, 5L).isActive());
        assertEquals(1, categoryService.list(null, false).size());
        assertEquals(0, categoryService.list(null, true).size());
        assertTrue(categoryService.activate(teacher, 5L).isActive());
        assertThrows(AccessDeniedException.class, () -> categoryService.deactivate(student, 5L));
    }
}
