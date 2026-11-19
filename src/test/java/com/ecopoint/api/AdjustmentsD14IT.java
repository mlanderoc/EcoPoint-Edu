package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Routes of the D14 adjustments against the demo data: the crew of a study
 * and the studies a user may register in (plan D14, tasks B08 and B09).
 *
 * @author craulgomez
 * @version 1.0
 */
@AutoConfigureMockMvc
class AdjustmentsD14IT extends PostgresIntegrationTest {

    private static final String PASSWORD = "EcoPoint2026";
    private static final String TEACHER = "docente.demo@unicesar.edu.co";
    private static final String STUDENT = "estudiante.demo@unicesar.edu.co";

    @Autowired
    private MockMvc mvc;

    private Long running;

    @BeforeEach
    void loadDemoData() {
        runScript("002_seed.sql");
        running = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'EN_EJECUCION'", Long.class);
    }

    @Test
    void ca213_studentOutsideTheCrewNeitherSeesNorRegistersInTheStudy() throws Exception {
        ApiClient student = ApiClient.loggedIn(mvc, STUDENT, PASSWORD);
        Long shift = jdbc.queryForObject("SELECT id FROM TURNO WHERE id_estudio = ? AND estado = 'ABIERTO' LIMIT 1",
                Long.class, running);
        Long source = jdbc.queryForObject("SELECT id_fuente FROM FUENTE_ESTUDIO WHERE id_estudio = ? LIMIT 1",
                Long.class, running);
        String bag = "{\"sourceId\":" + source + ",\"grossWeight\":12.40,\"tare\":0.40,\"volume\":0.080}";

        student.perform(get("/api/v1/studies/field")).andExpect(jsonPath("$[0].id").value(running));

        jdbc.update("DELETE FROM CUADRILLA_INTEGRANTE WHERE id_usuario = (SELECT id FROM USUARIO WHERE correo = ?)",
                STUDENT);

        student.perform(get("/api/v1/studies/field")).andExpect(jsonPath("$", hasSize(0)));
        student.json(post("/api/v1/shifts/" + shift + "/bags"), bag)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));
    }

    @Test
    void ca322_studyNeedsACrewAndTheShiftTakesIt() throws Exception {
        ApiClient teacher = ApiClient.loggedIn(mvc, TEACHER, PASSWORD);
        Long crew = jdbc.queryForObject("SELECT id FROM CUADRILLA LIMIT 1", Long.class);
        String days = "[{\"dayNumber\":1,\"date\":\"2027-02-01\"}]";

        teacher.json(post("/api/v1/studies"), "{\"days\":" + days + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("crewId"));
        teacher.json(post("/api/v1/studies"), "{\"crewId\":" + crew + ",\"days\":" + days + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.crewId").value(crew));

        teacher.perform(get("/api/v1/studies/field")).andExpect(jsonPath("$[0].id").value(running));
    }

    @Test
    void ca083_crewChangesOnlyWhilePlanned() throws Exception {
        ApiClient teacher = ApiClient.loggedIn(mvc, TEACHER, PASSWORD);
        Long other = jdbc.queryForObject("INSERT INTO CUADRILLA (nombre, id_responsable) "
                + "SELECT 'Cuadrilla 2', id FROM USUARIO WHERE correo = ? RETURNING id", Long.class, TEACHER);

        teacher.json(put("/api/v1/studies/" + running + "/crew"), "{\"crewId\":" + other + "}")
                .andExpect(status().isUnprocessableEntity());
    }
}
