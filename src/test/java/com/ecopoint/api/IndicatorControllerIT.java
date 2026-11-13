package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Indicators endpoint against the demo data (spec 006, T504 and T505;
 * CA-504, CA-505).
 *
 * @author craulgomez
 */
@AutoConfigureMockMvc
class IndicatorControllerIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "EcoPoint2026";

    @Autowired
    private MockMvc mvc;

    private ApiClient student;
    private long closedStudy;
    private long runningStudy;

    @BeforeEach
    void loadDemoData() throws Exception {
        runScript("002_seed.sql");
        student = ApiClient.loggedIn(mvc, "estudiante.demo@unicesar.edu.co", PASSWORD);
        closedStudy = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'CERRADO'", Long.class);
        runningStudy = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'EN_EJECUCION'", Long.class);
    }

    @Test
    void ca504_preliminaryStatusWithVerificationPercentageWhenStudyNotClosed() throws Exception {
        jdbc.update("UPDATE ESTUDIO SET estado = 'EN_DEPURACION' WHERE id = ?", runningStudy);

        student.perform(get("/api/v1/studies/" + runningStudy + "/indicators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.official").value(false))
                .andExpect(jsonPath("$.label").value("Preliminar"))
                .andExpect(jsonPath("$.quality.verifiedPercentage", notNullValue()))
                .andExpect(jsonPath("$.days", hasSize(8)));
    }

    @Test
    void ca504_officialStatusWhenStudyClosed() throws Exception {
        student.perform(get("/api/v1/studies/" + closedStudy + "/indicators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.official").value(true))
                .andExpect(jsonPath("$.label").value("Oficial"))
                .andExpect(jsonPath("$.summary.countedDays").value(7))
                .andExpect(jsonPath("$.summary.averageDailyGeneration", notNullValue()))
                .andExpect(jsonPath("$.composition.groups", hasSize(4)));
    }

    @Test
    void ca505_quarteringCompletenessNoteReported() throws Exception {
        student.perform(get("/api/v1/studies/" + runningStudy + "/indicators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.composition.expectedShifts").value(14))
                .andExpect(jsonPath("$.composition.shiftsWithQuartering", lessThan(14)))
                .andExpect(jsonPath("$.composition.note", matchesPattern("Composición calculada con \\d+ de 14 turnos")));
    }

    @Test
    void ca207_unknownStudyIsNotFound() throws Exception {
        student.perform(get("/api/v1/studies/999999/indicators")).andExpect(status().isNotFound());
    }
}
