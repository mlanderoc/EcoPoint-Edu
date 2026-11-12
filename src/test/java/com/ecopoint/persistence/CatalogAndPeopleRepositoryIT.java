package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.Crew;
import com.ecopoint.model.Institution;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests of {@link WasteCategoryRepository}, {@link UserRepository}
 * and {@link CrewRepository} against PostgreSQL (CA-110, RN-041, RN-042, RN-084).
 *
 * @author mlanderoc
 * @version 1.0
 */
class CatalogAndPeopleRepositoryIT extends PostgresIntegrationTest {

    @Autowired
    private WasteCategoryRepository categories;

    @Autowired
    private UserRepository users;

    @Autowired
    private CrewRepository crews;

    @Autowired
    private InstitutionRepository institutions;

    private Long institutionId;

    @BeforeEach
    void createInstitution() {
        institutionId = institutions.save(new Institution(null, "I.E. Prudencia Daza", "Valledupar")).getId();
    }

    private User teacher() {
        return users.save(new User(null, "Oriana", "Jurado", "oriana@unicesar.edu.co", UserRole.DOCENTE, institutionId));
    }

    private User student(String firstName, String email) {
        return users.save(new User(null, firstName, "Prueba", email, UserRole.ESTUDIANTE_UNIVERSITARIO, institutionId));
    }

    @Test
    void ca110_wasteCategoryCrud() {
        WasteCategory cardboard = categories.save(new WasteCategory(null, "Cartón", WasteGroup.BLANCO, "Cajas"));
        categories.save(new WasteCategory(null, "Pilas", WasteGroup.SIN_COLOR, null));

        cardboard.setDescription("Cajas y empaques");
        cardboard.deactivate();
        categories.save(cardboard);

        WasteCategory found = categories.findById(cardboard.getId()).orElseThrow();
        assertEquals("Cajas y empaques", found.getDescription());
        assertFalse(found.isActive());
        assertEquals(WasteGroup.BLANCO, categories.findAll().get(0).getGroup());
        Map<Long, WasteCategory> catalog = categories.findAllAsMap();
        assertEquals(2, catalog.size());
        assertTrue(categories.findNames().contains("Pilas"));
    }

    @Test
    void rn041_duplicatedSubcategoryNameIsRejectedIgnoringCase() {
        categories.save(new WasteCategory(null, "Cartón", WasteGroup.BLANCO, null));

        assertThrows(PersistenceException.class,
                () -> categories.save(new WasteCategory(null, " cartón ", WasteGroup.BLANCO, null)));
    }

    @Test
    void rn042_hasResultsIsTrueForCategoriesUsedInAQuartering() {
        runScript("002_seed.sql");
        Long paper = jdbc.queryForObject("SELECT id FROM SUBCATEGORIA_RESIDUO WHERE nombre = 'Papel'", Long.class);
        WasteCategory unused = categories.save(new WasteCategory(null, "Textiles", WasteGroup.NEGRO, null));

        assertTrue(categories.hasResults(paper));
        assertFalse(categories.hasResults(unused.getId()));
    }

    @Test
    void ca110_userCrudAndSearchByEmail() {
        User saved = teacher();
        saved.setLastName("Jurado Fuentes");
        users.save(saved);
        student("Laura", "laura@unicesar.edu.co");

        assertEquals("Jurado Fuentes", users.findById(saved.getId()).orElseThrow().getLastName());
        assertEquals(saved.getId(), users.findByEmail(" ORIANA@unicesar.edu.co").orElseThrow().getId());
        assertEquals(2, users.findAll().size());
        assertEquals(2, users.findByInstitution(institutionId).size());
        assertTrue(users.findEmails().contains("laura@unicesar.edu.co"));

        users.deactivate(saved.getId());
        assertFalse(users.findById(saved.getId()).orElseThrow().isActive());
    }

    @Test
    void rn084_duplicatedEmailIsRejectedIgnoringCase() {
        teacher();

        assertThrows(PersistenceException.class,
                () -> users.save(new User(null, "Otra", "Persona", "Oriana@UNICESAR.edu.co", UserRole.DOCENTE, institutionId)));
    }

    @Test
    void ca110_crewIsSavedWithItsMembersAndUpdatedInOneTransaction() {
        User teacher = teacher();
        User laura = student("Laura", "laura@unicesar.edu.co");
        User andres = student("Andrés", "andres@unicesar.edu.co");
        Crew crew = new Crew(null, "Cuadrilla 1", teacher);
        crew.addMember(laura);
        crew.addMember(andres);

        crews.save(crew);
        crew.removeMember(andres);
        crew.setName("Cuadrilla mañana");
        crews.save(crew);

        Crew found = crews.findById(crew.getId()).orElseThrow();
        assertEquals("Cuadrilla mañana", found.getName());
        assertEquals(teacher.getId(), found.getResponsible().getId());
        assertTrue(found.hasResponsibleTeacher());
        assertEquals(List.of(laura.getId()), found.getMembers().stream().map(User::getId).toList());
        assertEquals(1, crews.findByInstitution(institutionId).size());
    }
}
