package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Complete field flow through the API against the demo data: open a shift,
 * register its population and a bag, mark the other sources without generation
 * and close it (spec 003, T15; CA-204, CA-205, CA-206, CA-207).
 *
 * @author craulgomez
 */
@AutoConfigureMockMvc
class FieldFlowIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "EcoPoint2026";
    private static final long RUNNING_STUDY = 2L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private ApiClient teacher;
    private ApiClient student;

    @BeforeEach
    void loadDemoDataAndLogIn() throws Exception {
        runScript("002_seed.sql");
        teacher = ApiClient.loggedIn(mvc, "docente.demo@unicesar.edu.co", PASSWORD);
        student = ApiClient.loggedIn(mvc, "estudiante.demo@unicesar.edu.co", PASSWORD);
    }

    @Test
    void r340_runningStudyShowsItsSixteenShifts() throws Exception {
        teacher.perform(get("/api/v1/studies/" + RUNNING_STUDY + "/shifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(jsonPath("$[0].shiftId").isNumber())
                .andExpect(jsonPath("$[15].shiftId").doesNotExist());
    }

    @Test
    void ca207_missingStudyIsNotFound() throws Exception {
        teacher.perform(get("/api/v1/studies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ERR_NOT_FOUND"));
    }

    @Test
    void ca204_studentCannotOpenShifts() throws Exception {
        student.json(post("/api/v1/studies/" + RUNNING_STUDY + "/shifts"), "{\"dayNumber\":3,\"type\":\"MANANA\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));
    }

    @Test
    void ca206_bagBeforePopulationIsABusinessRuleError() throws Exception {
        long shift = openShift();

        student.json(post("/api/v1/shifts/" + shift + "/bags"), bag(firstSource(shift), "12.40", "0.40"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("ERR_BUSINESS_RULE"))
                .andExpect(jsonPath("$.message").value("Registre primero la población del turno."));
    }

    @Test
    void ca205_tareGreaterThanGrossWeightPointsToTheTareField() throws Exception {
        long shift = openShift();
        registerPopulation(shift);

        student.json(post("/api/v1/shifts/" + shift + "/bags"), bag(firstSource(shift), "3.20", "3.50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR_INVALID_DATA"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("tare"));
    }

    @Test
    void ca032_completeShiftFromOpeningToClosing() throws Exception {
        long shift = openShift();
        registerPopulation(shift);
        List<Long> sources = sourceIds(shift);

        student.json(post("/api/v1/shifts/" + shift + "/bags"), bag(sources.get(0), "12.40", "0.40"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value(org.hamcrest.Matchers.endsWith("-D3-M")))
                .andExpect(jsonPath("$.netWeight").value(12.00))
                .andExpect(jsonPath("$.density").value(150.00))
                .andExpect(jsonPath("$.verification.status").value("PENDIENTE"));

        teacher.perform(post("/api/v1/shifts/" + shift + "/close"))
                .andExpect(status().isUnprocessableEntity());

        for (Long source : sources.subList(1, sources.size())) {
            student.perform(post("/api/v1/shifts/" + shift + "/sources/" + source + "/no-generation"))
                    .andExpect(status().isOk());
        }

        teacher.perform(post("/api/v1/shifts/" + shift + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CERRADO"))
                .andExpect(jsonPath("$.bagCount").value(1))
                .andExpect(jsonPath("$.totalNetWeight").value(12.00))
                .andExpect(jsonPath("$.sources[0].status").value("CON_GENERACION"))
                .andExpect(jsonPath("$.sources[1].status").value("SIN_GENERACION"));
    }

    private long openShift() throws Exception {
        String body = teacher.json(post("/api/v1/studies/" + RUNNING_STUDY + "/shifts"),
                        "{\"dayNumber\":3,\"type\":\"MANANA\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("id").asLong();
    }

    private void registerPopulation(long shift) throws Exception {
        student.json(put("/api/v1/shifts/" + shift + "/population"), "{\"students\":300,\"teachers\":12,\"staff\":6}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(318));
    }

    private List<Long> sourceIds(long shift) throws Exception {
        String body = teacher.perform(get("/api/v1/shifts/" + shift)).andReturn().getResponse().getContentAsString();
        JsonNode sources = json.readTree(body).path("sources");
        return java.util.stream.StreamSupport.stream(sources.spliterator(), false)
                .map(node -> node.path("sourceId").asLong())
                .toList();
    }

    private long firstSource(long shift) throws Exception {
        return sourceIds(shift).get(0);
    }

    private static String bag(long source, String gross, String tare) {
        return "{\"sourceId\":" + source + ",\"grossWeight\":" + gross + ",\"tare\":" + tare + ",\"volume\":0.080}";
    }
}
