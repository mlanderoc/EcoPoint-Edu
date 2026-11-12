package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.GeoPoint;
import com.ecopoint.model.Institution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests of {@link InstitutionRepository} against PostgreSQL (CA-110).
 *
 * @author mlanderoc
 * @version 1.0
 */
class InstitutionRepositoryIT extends PostgresIntegrationTest {

    @Autowired
    private InstitutionRepository repository;

    private Institution pilot() {
        Institution institution = new Institution(null, "I.E. Prudencia Daza", "Valledupar, Cesar");
        institution.setMapCenter(GeoPoint.of("10.4700", "-73.2500"));
        institution.setMapZoom(18);
        return institution;
    }

    @Test
    void ca110_saveAndFindById() {
        Institution saved = repository.save(pilot());

        assertNotNull(saved.getId());
        Institution found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("I.E. Prudencia Daza", found.getName());
        assertEquals(GeoPoint.of("10.4700", "-73.2500"), found.getMapCenter());
        assertEquals(18, found.getMapZoom());
        assertEquals(CharacterizationSettings.defaults().minSampleWeight().compareTo(
                found.getSettings().minSampleWeight()), 0);
    }

    @Test
    void ca110_updateKeepsTheSameRowAndChangesTheSettings() {
        Institution saved = repository.save(pilot());
        saved.setAddress("Calle de prueba, Valledupar");
        saved.setSettings(new CharacterizationSettings(new BigDecimal("40"), new BigDecimal("60"),
                new BigDecimal("3"), new BigDecimal("45"), new BigDecimal("25"), new BigDecimal("900")));

        repository.save(saved);

        Institution found = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Calle de prueba, Valledupar", found.getAddress());
        assertEquals(0, new BigDecimal("3").compareTo(found.getSettings().qualityTolerancePercent()));
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void ca110_findAllAndExistsByName() {
        repository.save(pilot());
        repository.save(new Institution(null, "Colegio de prueba", "Valledupar"));

        assertEquals(2, repository.findAll().size());
        assertEquals("Colegio de prueba", repository.findAll().get(0).getName());
        assertTrue(repository.existsByName("i.e. prudencia daza "));
        assertFalse(repository.existsByName("Otra"));
        assertTrue(repository.findById(999L).isEmpty());
    }

    @Test
    void r021_duplicatedNameIsReportedAsPersistenceException() {
        repository.save(pilot());

        assertThrows(PersistenceException.class, () -> repository.save(pilot()));
    }
}
