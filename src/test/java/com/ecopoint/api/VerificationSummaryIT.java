package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Data cleaning summary and record queries against the demo data (spec 005,
 * T401; CA-410, R-420, R-421, R-430).
 *
 * @author craulgomez
 */
@AutoConfigureMockMvc
class VerificationSummaryIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "EcoPoint2026";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private ApiClient teacher;
    private long runningStudy;

    @BeforeEach
    void loadDemoData() throws Exception {
        runScript("002_seed.sql");
        teacher = ApiClient.loggedIn(mvc, "docente.demo@unicesar.edu.co", PASSWORD);
        runningStudy = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'EN_EJECUCION'", Long.class);
    }

    @Test
    void ca410_studyIsReadyToCloseOnlyWhenNothingIsPending() throws Exception {
        String body = teacher.perform(get("/api/v1/studies/" + runningStudy + "/verification-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending", greaterThan(0)))
                .andExpect(jsonPath("$.readyToClose").value(false))
                .andReturn().getResponse().getContentAsString();

        for (JsonNode shift : json.readTree(body).path("shifts")) {
            String records = teacher.perform(get("/api/v1/shifts/" + shift.path("shiftId").asLong() + "/records"))
                    .andReturn().getResponse().getContentAsString();
            for (JsonNode record : json.readTree(records)) {
                if ("PENDIENTE".equals(record.path("verification").path("status").asText())) {
                    teacher.perform(post("/api/v1/verifications/" + record.path("recordType").asText() + "/"
                            + record.path("recordId").asLong())).andExpect(status().isOk());
                }
            }
        }

        teacher.perform(get("/api/v1/studies/" + runningStudy + "/verification-summary"))
                .andExpect(jsonPath("$.pending").value(0))
                .andExpect(jsonPath("$.readyToClose").value(true));
    }

    @Test
    void r421_shiftRecordsShowTheirTypedValuesAndWhichOnesCanBeCorrected() throws Exception {
        Long shift = jdbc.queryForObject("SELECT id FROM TURNO WHERE id_estudio = ? AND estado = 'ABIERTO'",
                Long.class, runningStudy);

        String records = teacher.perform(get("/api/v1/shifts/" + shift + "/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recordType").value("population"))
                .andExpect(jsonPath("$[*].recordType", hasItem("bag")))
                .andReturn().getResponse().getContentAsString();
        JsonNode bag = null;
        for (JsonNode record : json.readTree(records)) {
            if ("bag".equals(record.path("recordType").asText())) {
                bag = record;
                break;
            }
        }
        JsonNode gross = null;
        for (JsonNode field : bag.path("fields")) {
            if ("grossWeight".equals(field.path("field").asText())) {
                gross = field;
            }
        }
        assertEquals(true, gross.path("editable").asBoolean());
        assertEquals(shift.longValue(), bag.path("shiftId").asLong());

        teacher.perform(get("/api/v1/records/bag/" + bag.path("recordId").asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(bag.path("title").asText()))
                .andExpect(jsonPath("$.shiftCode").value(bag.path("shiftCode").asText()));
    }

    @Test
    void ca207_unknownRecordIsNotFound() throws Exception {
        teacher.perform(get("/api/v1/records/bag/999999")).andExpect(status().isNotFound());
        teacher.perform(get("/api/v1/records/photo/1")).andExpect(status().isBadRequest());
    }
}
