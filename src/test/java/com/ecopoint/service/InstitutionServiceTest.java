package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.GeoPoint;
import com.ecopoint.model.Institution;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.persistence.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Administration of institutions (R-320, R-623).
 *
 * @author EstefaniaMarquez
 */
class InstitutionServiceTest {

    private InstitutionRepository institutions;
    private InstitutionService service;
    private final User teacher = new User(10L, "Daniela", "Arias", "d@unicesar.edu.co", UserRole.DOCENTE, 1L);
    private final User student = new User(11L, "Laura", "Pérez", "l@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);

    @BeforeEach
    void setUp() {
        institutions = mock(InstitutionRepository.class);
        service = new InstitutionService(institutions);
        when(institutions.save(any())).thenAnswer(call -> call.getArgument(0));
        when(institutions.findById(1L)).thenReturn(Optional.of(new Institution(1L, "I.E. Prudencia Daza", "Valledupar")));
        when(institutions.existsByName("Colegio Nuevo")).thenReturn(false);
        when(institutions.existsByName("I.E. Prudencia Daza")).thenReturn(true);
    }

    @Test
    void r320_teacherCreatesAnInstitution() {
        Institution created = service.create(teacher, new Institution(7L, "Colegio Nuevo", "Calle 1"));

        assertNull(created.getId(), "El id lo asigna la base de datos");
        verify(institutions).save(created);
    }

    @Test
    void r320_repeatedNameIsRejected() {
        assertThrows(BusinessRuleException.class,
                () -> service.create(teacher, new Institution(null, "I.E. Prudencia Daza", "Otra dirección")));
    }

    @Test
    void r320_missingNameIsInvalid() {
        assertThrows(InvalidDataException.class, () -> service.create(teacher, new Institution(null, " ", "Calle 1")));
    }

    @Test
    void ca203_studentCannotCreateInstitutions() {
        assertThrows(AccessDeniedException.class,
                () -> service.create(student, new Institution(null, "Colegio Nuevo", "Calle 1")));
        verify(institutions, never()).save(any());
    }

    @Test
    void r623_updateKeepsTheNameAndSavesTheMapView() {
        Institution changes = new Institution(null, "I.E. Prudencia Daza", "Valledupar, Cesar");
        changes.setMapCenter(GeoPoint.of("10.47", "-73.25"));
        changes.setMapZoom(18);

        Institution saved = service.update(teacher, 1L, changes);

        assertEquals("Valledupar, Cesar", saved.getAddress());
        assertEquals(18, saved.getMapZoom());
        assertEquals(GeoPoint.of("10.47", "-73.25"), saved.getMapCenter());
    }

    @Test
    void ca207_missingInstitutionIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.get(99L));
    }

    @Test
    void rn006_institutionIsDeactivatedActivatedAndFilteredByState() {
        Institution pilot = institutions.findById(1L).orElseThrow();
        when(institutions.findAll()).thenReturn(List.of(pilot));

        assertFalse(service.deactivate(teacher, 1L).isActive());
        assertEquals(1, service.list(false).size());
        assertEquals(0, service.list(true).size());
        assertEquals(1, service.list(null).size());
        assertTrue(service.activate(teacher, 1L).isActive());
        assertThrows(AccessDeniedException.class, () -> service.deactivate(student, 1L));
    }
}
