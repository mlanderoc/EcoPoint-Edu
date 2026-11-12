package com.ecopoint.service;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Institution;
import com.ecopoint.model.SourceType;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.persistence.GenerationSourceRepository;
import com.ecopoint.persistence.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Administration of generation sources (RN-001 to RN-005, R-321).
 *
 * @author EstefaniaMarquez
 */
class GenerationSourceServiceTest {

    private static final Long INSTITUTION = 1L;

    private GenerationSourceRepository sources;
    private GenerationSourceService service;
    private final User teacher = new User(10L, "Daniela", "Arias", "d@unicesar.edu.co", UserRole.DOCENTE, INSTITUTION);
    private final User student = new User(11L, "Laura", "Pérez", "l@unicesar.edu.co",
            UserRole.ESTUDIANTE_UNIVERSITARIO, INSTITUTION);
    private GenerationSource store;

    @BeforeEach
    void setUp() {
        sources = mock(GenerationSourceRepository.class);
        InstitutionRepository institutions = mock(InstitutionRepository.class);
        service = new GenerationSourceService(sources, institutions);
        when(institutions.findById(INSTITUTION)).thenReturn(Optional.of(new Institution(INSTITUTION, "I.E.", "Valledupar")));
        store = new GenerationSource(5L, INSTITUTION, "TI", "Tienda escolar", SourceType.TIENDA_ESCOLAR);
        when(sources.findById(5L)).thenReturn(Optional.of(store));
        when(sources.findCodesByInstitution(INSTITUTION)).thenReturn(Set.of("TI", "PAE"));
        when(sources.findByInstitution(INSTITUTION)).thenReturn(List.of(store));
        when(sources.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void r321_teacherCreatesASource() {
        GenerationSource created = service.create(teacher, INSTITUTION, "LAB", "Laboratorio", SourceType.LABORATORIO);

        assertEquals("LAB", created.getCode());
        verify(sources).save(created);
    }

    @Test
    void ca002_repeatedCodeIsRejected() {
        assertThrows(BusinessRuleException.class,
                () -> service.create(teacher, INSTITUTION, "PAE", "Otro PAE", SourceType.PAE));
    }

    @Test
    void ca001_codeWithInvalidFormatIsRejected() {
        assertThrows(InvalidDataException.class,
                () -> service.create(teacher, INSTITUTION, "aulas12", "Aulas", SourceType.AULAS));
    }

    @Test
    void rn001_sourceOfAMissingInstitutionIsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.create(teacher, 99L, "LAB", "Laboratorio", SourceType.LABORATORIO));
    }

    @Test
    void ca203_studentCannotManageSources() {
        assertThrows(AccessDeniedException.class,
                () -> service.create(student, INSTITUTION, "LAB", "Laboratorio", SourceType.LABORATORIO));
        assertThrows(AccessDeniedException.class, () -> service.deactivate(student, INSTITUTION, 5L));
        verify(sources, never()).save(any());
    }

    @Test
    void rn002_updatingASourceMayKeepItsOwnCode() {
        GenerationSource updated = service.update(teacher, INSTITUTION, 5L, "TI", "Tienda", SourceType.TIENDA_ESCOLAR);

        assertEquals("Tienda", updated.getName());
    }

    @Test
    void rn002_updatingToTheCodeOfAnotherSourceIsRejected() {
        assertThrows(BusinessRuleException.class,
                () -> service.update(teacher, INSTITUTION, 5L, "PAE", "Tienda", SourceType.TIENDA_ESCOLAR));
    }

    @Test
    void rn005_sourceWithBagsIsOnlyDeactivated() {
        when(sources.hasBags(5L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.delete(teacher, INSTITUTION, 5L));
        verify(sources, never()).delete(5L);

        assertFalse(service.deactivate(teacher, INSTITUTION, 5L).isActive());
    }

    @Test
    void rn005_sourceWithoutBagsCanBeDeleted() {
        service.delete(teacher, INSTITUTION, 5L);

        verify(sources).delete(5L);
    }

    @Test
    void rn005_inactiveSourcesAreLeftOutWhenAskingOnlyActiveOnes() {
        store.deactivate();

        assertEquals(0, service.list(INSTITUTION, true).size());
        assertEquals(1, service.list(INSTITUTION, false).size());
    }

    @Test
    void rn001_sourceOfAnotherInstitutionIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.get(2L, 5L));
    }
}
