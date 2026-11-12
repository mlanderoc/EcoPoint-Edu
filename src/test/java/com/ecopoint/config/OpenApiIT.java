package com.ecopoint.config;

import com.ecopoint.persistence.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OpenAPI document of the API with the {@code dev} profile (spec 003, CA-212).
 *
 * @author mlanderoc
 */
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class OpenApiIT extends PostgresIntegrationTest {

    /** Routes of sections 5.1 to 5.3 of the plan; 5.4 belongs to specs 006 and 007. */
    private static final List<String> RESOURCES = List.of(
            "/api/v1/auth/login", "/api/v1/auth/logout", "/api/v1/auth/me", "/api/v1/auth/csrf",
            "/api/v1/institutions", "/api/v1/institutions/{id}",
            "/api/v1/institutions/{institutionId}/sources", "/api/v1/institutions/{institutionId}/sources/{sourceId}",
            "/api/v1/users", "/api/v1/users/{id}", "/api/v1/crews", "/api/v1/crews/{id}",
            "/api/v1/waste-categories", "/api/v1/waste-categories/{id}",
            "/api/v1/studies", "/api/v1/studies/{id}", "/api/v1/studies/{id}/days", "/api/v1/studies/{id}/sources",
            "/api/v1/studies/{id}/status", "/api/v1/studies/{id}/shifts",
            "/api/v1/shifts/{shiftId}", "/api/v1/shifts/{shiftId}/close", "/api/v1/shifts/{shiftId}/reopen",
            "/api/v1/shifts/{shiftId}/population", "/api/v1/shifts/{shiftId}/bags",
            "/api/v1/shifts/{shiftId}/sources/{sourceId}/no-generation", "/api/v1/bags/{bagId}",
            "/api/v1/shifts/{shiftId}/quartering", "/api/v1/shifts/{shiftId}/quartering/annul",
            "/api/v1/shifts/{shiftId}/density", "/api/v1/verifications",
            "/api/v1/verifications/{recordType}/{recordId}", "/api/v1/verifications/{recordType}/{recordId}/correct",
            "/api/v1/records/{recordType}/{recordId}/history", "/api/v1/evidences", "/api/v1/evidences/{id}/file",
            "/api/v1/alerts", "/api/v1/alerts/{id}/attend");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void ca212_documentListsEveryResourceOfTheApi() throws Exception {
        String body = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode document = json.readTree(body);

        assertEquals("EcoPoint Edu API", document.path("info").path("title").asText());
        JsonNode paths = document.path("paths");
        List<String> missing = RESOURCES.stream().filter(path -> !paths.has(path)).toList();
        assertTrue(missing.isEmpty(), "Faltan en OpenAPI: " + missing);
    }

    @Test
    void r130_swaggerUiIsAvailableInDevelopment() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
