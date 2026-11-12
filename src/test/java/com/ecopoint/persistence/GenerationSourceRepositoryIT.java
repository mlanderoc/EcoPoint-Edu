package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.GeoPoint;
import com.ecopoint.model.GeoPolygon;
import com.ecopoint.model.Institution;
import com.ecopoint.model.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests of {@link GenerationSourceRepository} against PostgreSQL
 * (CA-110, CA-104 from the application, R-032).
 *
 * @author mlanderoc
 * @version 1.0
 */
class GenerationSourceRepositoryIT extends PostgresIntegrationTest {

    private static final GeoPoint A = GeoPoint.of("10.4703", "-73.2506");
    private static final GeoPoint B = GeoPoint.of("10.4703", "-73.2503");
    private static final GeoPoint C = GeoPoint.of("10.4705", "-73.2503");

    @Autowired
    private GenerationSourceRepository repository;

    @Autowired
    private InstitutionRepository institutions;

    private Long institutionId;

    @BeforeEach
    void createInstitution() {
        institutionId = institutions.save(new Institution(null, "I.E. Prudencia Daza", "Valledupar")).getId();
    }

    private GenerationSource classrooms() {
        GenerationSource source = new GenerationSource(null, institutionId, "AU1", "Bloque de aulas 1", SourceType.AULAS);
        source.setGeometry(new GeoPolygon(List.of(A, B, C, A)));
        return source;
    }

    @Test
    void r032_polygonIsStoredAsGeoJsonAndReadBack() {
        GenerationSource saved = repository.save(classrooms());

        GenerationSource found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("AU1", found.getCode());
        assertEquals(SourceType.AULAS, found.getType());
        assertEquals(new GeoPolygon(List.of(A, B, C, A)), found.getGeometry());
        assertEquals("Polygon", jdbc.queryForObject("SELECT geometria ->> 'type' FROM FUENTE_GENERACION", String.class));
    }

    @Test
    void ca110_sourceWithoutPolygonUpdateAndList() {
        GenerationSource office = repository.save(
                new GenerationSource(null, institutionId, "OF", "Oficinas", SourceType.OFICINAS));
        assertNull(repository.findById(office.getId()).orElseThrow().getGeometry());

        office.setName("Oficinas administrativas");
        repository.save(office);
        repository.save(classrooms());

        List<GenerationSource> list = repository.findByInstitution(institutionId);
        assertEquals(2, list.size());
        assertEquals("AU1", list.get(0).getCode());
        assertEquals("Oficinas administrativas", list.get(1).getName());
        assertEquals(Set.of("AU1", "OF"), repository.findCodesByInstitution(institutionId));
    }

    @Test
    void rn005_deactivateKeepsTheSourceButMarksItInactive() {
        GenerationSource saved = repository.save(classrooms());

        repository.deactivate(saved.getId());

        assertFalse(repository.findById(saved.getId()).orElseThrow().isActive());
    }

    @Test
    void ca104_sourceWithBagsCannotBeDeletedButOneWithoutBagsCan() {
        runScript("002_seed.sql");
        Long pae = jdbc.queryForObject("SELECT id FROM FUENTE_GENERACION WHERE codigo = 'PAE'", Long.class);
        Long institution = jdbc.queryForObject("SELECT id FROM INSTITUCION", Long.class);
        GenerationSource unused = repository.save(
                new GenerationSource(null, institution, "XX", "Fuente sin uso", SourceType.OTRA));

        assertTrue(repository.hasBags(pae));
        assertThrows(PersistenceException.class, () -> repository.delete(pae));
        assertFalse(repository.hasBags(unused.getId()));
        repository.delete(unused.getId());
        assertTrue(repository.findById(unused.getId()).isEmpty());
    }

    @Test
    void rn002_duplicatedCodeInTheSameInstitutionIsRejected() {
        repository.save(classrooms());

        assertThrows(PersistenceException.class, () -> repository.save(classrooms()));
    }
}
