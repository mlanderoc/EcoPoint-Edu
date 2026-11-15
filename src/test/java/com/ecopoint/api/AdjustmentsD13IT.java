package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Routes of the D13 adjustments against the demo data: cancel and delete
 * studies, activate and deactivate, users table and profile (plan D13,
 * section 4; task A07).
 *
 * @author craulgomez
 * @version 1.0
 */
@AutoConfigureMockMvc
class AdjustmentsD13IT extends PostgresIntegrationTest {

    private static final String PASSWORD = "EcoPoint2026";
    private static final String TEACHER = "docente.demo@unicesar.edu.co";
    private static final String STUDENT = "estudiante.demo@unicesar.edu.co";

    @Autowired
    private MockMvc mvc;

    private Long institution;

    @BeforeEach
    void loadDemoData() {
        runScript("002_seed.sql");
        institution = jdbc.queryForObject("SELECT id FROM INSTITUCION", Long.class);
    }

    @Test
    void ca015_teacherCancelsTheExecutingStudyAndItsDatesBecomeFree() throws Exception {
        ApiClient teacher = ApiClient.loggedIn(mvc, TEACHER, PASSWORD);
        ApiClient student = ApiClient.loggedIn(mvc, STUDENT, PASSWORD);
        Long executing = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'EN_EJECUCION'", Long.class);
        String cancel = "/api/v1/studies/" + executing + "/cancel";

        student.json(post(cancel), "{\"reason\":\"Fechas equivocadas\"}").andExpect(status().isForbidden());
        teacher.json(post(cancel), "{\"reason\":\" \"}").andExpect(status().isBadRequest());
        teacher.json(post(cancel), "{\"reason\":\"Fechas equivocadas\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.cancellationReason").value("Fechas equivocadas"));

        String days = jdbc.queryForList("SELECT '{\"dayNumber\":' || numero_dia || ',\"date\":\"' || fecha || '\"}' "
                + "FROM DIA_MUESTREO WHERE id_estudio = ? ORDER BY numero_dia", String.class, executing).toString();
        teacher.json(post("/api/v1/studies"), "{\"institutionId\":" + institution + ",\"days\":" + days + "}")
                .andExpect(status().isCreated());
    }

    @Test
    void ca016_closedStudyIsNotCancelledAndOnlyAPlannedOneIsDeleted() throws Exception {
        ApiClient teacher = ApiClient.loggedIn(mvc, TEACHER, PASSWORD);
        Long closed = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'CERRADO'", Long.class);

        teacher.json(post("/api/v1/studies/" + closed + "/cancel"), "{\"reason\":\"Motivo\"}")
                .andExpect(status().isUnprocessableEntity());
        teacher.perform(delete("/api/v1/studies/" + closed)).andExpect(status().isUnprocessableEntity());

        Long planned = jdbc.queryForObject("INSERT INTO ESTUDIO (id_institucion) VALUES (?) RETURNING id", Long.class,
                institution);
        jdbc.update("INSERT INTO DIA_MUESTREO (id_estudio, numero_dia) SELECT ?, n FROM generate_series(1, 8) n",
                planned);
        teacher.perform(delete("/api/v1/studies/" + planned)).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM ESTUDIO WHERE id = ?", Integer.class, planned));
    }

    @Test
    void ca005_inactiveInstitutionIsFilteredAndAdmitsNoNewStudies() throws Exception {
        ApiClient teacher = ApiClient.loggedIn(mvc, TEACHER, PASSWORD);

        teacher.perform(post("/api/v1/institutions/" + institution + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        teacher.perform(get("/api/v1/institutions").param("active", "true"))
                .andExpect(jsonPath("$.length()").value(0));
        teacher.json(post("/api/v1/studies"), "{\"institutionId\":" + institution + ",\"days\":[]}")
                .andExpect(status().isUnprocessableEntity());
        teacher.perform(post("/api/v1/institutions/" + institution + "/activate"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void ca312_sourceAndSubcategoryAreCreatedInactiveWhenTheFormSaysSo() throws Exception {
        ApiClient teacher = ApiClient.loggedIn(mvc, TEACHER, PASSWORD);
        String sources = "/api/v1/institutions/" + institution + "/sources";

        teacher.json(post(sources), "{\"code\":\"CAF\",\"name\":\"Cafetería\",\"type\":\"OTRA\",\"active\":false}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(false));
        teacher.perform(get(sources).param("active", "false"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CAF"));

        String created = teacher.json(post("/api/v1/waste-categories"),
                        "{\"name\":\"Colillas\",\"group\":\"NEGRO\",\"active\":false}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(false))
                .andReturn().getResponse().getContentAsString();
        Long category = Long.valueOf(created.replaceAll(".*\"id\":(\\d+).*", "$1"));
        teacher.perform(post("/api/v1/waste-categories/" + category + "/activate"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void ca313_usersTableIsFilteredSearchedAndOnlyStudentsAreDeactivated() throws Exception {
        ApiClient teacher = ApiClient.loggedIn(mvc, TEACHER, PASSWORD);
        Long teacherId = jdbc.queryForObject("SELECT id FROM USUARIO WHERE correo = ?", Long.class, TEACHER);
        Long studentId = jdbc.queryForObject("SELECT id FROM USUARIO WHERE correo = ?", Long.class, STUDENT);

        teacher.perform(get("/api/v1/users").param("q", "estudiante.demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].activity.typedRecords").value(greaterThan(0)));

        teacher.perform(post("/api/v1/users/" + teacherId + "/deactivate"))
                .andExpect(status().isUnprocessableEntity());
        teacher.perform(post("/api/v1/users/" + studentId + "/deactivate"))
                .andExpect(jsonPath("$.active").value(false));
        teacher.perform(get("/api/v1/users").param("role", "ESTUDIANTE_UNIVERSITARIO").param("active", "false"))
                .andExpect(jsonPath("$.items[0].email").value(STUDENT));
        teacher.perform(post("/api/v1/users/" + studentId + "/activate"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void ca314_profileShowsTheOwnTotalsAndThePasswordChangesWithTheCurrentOne() throws Exception {
        ApiClient student = ApiClient.loggedIn(mvc, STUDENT, PASSWORD);
        int bags = jdbc.queryForObject("SELECT count(*) FROM BOLSA b JOIN USUARIO u ON u.id = b.digitado_por "
                + "WHERE u.correo = ?", Integer.class, STUDENT);

        student.perform(get("/api/v1/auth/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(STUDENT))
                .andExpect(jsonPath("$.user.institutionName").value("I.E. Prudencia Daza"))
                .andExpect(jsonPath("$.activity.bags").value(bags));
        student.perform(get("/api/v1/users")).andExpect(status().isForbidden());

        student.json(post("/api/v1/auth/me/password"),
                        "{\"currentPassword\":\"Equivocada1\",\"newPassword\":\"NuevaClave2026\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"));
        student.json(post("/api/v1/auth/me/password"),
                        "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NuevaClave2026\"}")
                .andExpect(status().isNoContent());
        ApiClient.anonymous(mvc).login(STUDENT, "NuevaClave2026").andExpect(status().isOk());
    }
}
