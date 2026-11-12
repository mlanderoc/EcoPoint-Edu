package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matrix of permissions RN-081 through the API (spec 003, CA-203, CA-204 and
 * CA-211). The 3 permissions that only the teacher holds are checked with a
 * "yes" for the teacher and a 403 for the student. The other 8 are held by both
 * roles (D9), so both get a "yes"; there is no role that should get a "no".
 * VIEW_INDICATORS is checked by spec 006, which creates its route.
 *
 * @author craulgomez
 */
@AutoConfigureMockMvc
class PermissionMatrixIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "EcoPoint2026";
    private static final String TEACHER = "docente.demo@unicesar.edu.co";
    private static final String STUDENT = "estudiante.demo@unicesar.edu.co";
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
        teacher = ApiClient.loggedIn(mvc, TEACHER, PASSWORD);
        student = ApiClient.loggedIn(mvc, STUDENT, PASSWORD);
    }

    @Test
    void ca211_manageAdministrationOnlyForTheTeacher() throws Exception {
        String institution = "{\"name\":\"Colegio %s\",\"address\":\"Valledupar\"}";

        teacher.json(post("/api/v1/institutions"), institution.formatted("Docente")).andExpect(status().isCreated());
        student.json(post("/api/v1/institutions"), institution.formatted("Estudiante"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));
    }

    @Test
    void ca203_manageStudiesOnlyForTheTeacher() throws Exception {
        teacher.json(post("/api/v1/studies"), studyIn(2027)).andExpect(status().isCreated());
        student.json(post("/api/v1/studies"), studyIn(2028))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));
    }

    @Test
    void ca204_manageShiftsOnlyForTheTeacherEvenWhenTheShiftCanBeClosed() throws Exception {
        student.json(post("/api/v1/studies/" + RUNNING_STUDY + "/shifts"), "{\"dayNumber\":4,\"type\":\"MANANA\"}")
                .andExpect(status().isForbidden());

        long shift = openShift(4, "MANANA");
        coverEverySource(shift);

        student.perform(post("/api/v1/shifts/" + shift + "/close"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));
        teacher.perform(post("/api/v1/shifts/" + shift + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CERRADO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {TEACHER, STUDENT})
    void ca211_fieldPermissionsAreHeldByBothRoles(String email) throws Exception {
        ApiClient user = ApiClient.loggedIn(mvc, email, PASSWORD);
        long shift = openShift(TEACHER.equals(email) ? 5 : 6, "MANANA");
        long source = sources(shift).get(0);

        // REGISTER_POPULATION
        user.json(put("/api/v1/shifts/" + shift + "/population"), "{\"students\":300,\"teachers\":12,\"staff\":6}")
                .andExpect(status().isOk());
        // REGISTER_BAGS
        for (int i = 0; i < 6; i++) {
            user.json(post("/api/v1/shifts/" + shift + "/bags"),
                            "{\"sourceId\":" + source + ",\"grossWeight\":12.40,\"tare\":0.40,\"volume\":0.080}")
                    .andExpect(status().isCreated());
        }
        // REGISTER_QUARTERING (density) and REGISTER_HAZARDOUS_WASTE (SIN_COLOR result, RN-056)
        user.json(post("/api/v1/shifts/" + shift + "/density"), "{\"containerVolume\":0.200,\"netWeight\":30.00}")
                .andExpect(status().isCreated());
        user.json(post("/api/v1/shifts/" + shift + "/quartering"), hazardousNonConformingQuartering())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qualityStatus").value("NO_CONFORME"));
        // VERIFY_RECORDS
        user.perform(get("/api/v1/verifications").param("studyId", String.valueOf(RUNNING_STUDY)))
                .andExpect(status().isOk());
        // ATTEND_ALERTS
        long alert = firstPendingAlert();
        user.json(post("/api/v1/alerts/" + alert + "/attend"), "{\"reason\":\"Se revisó la submuestra.\"}")
                .andExpect(status().isOk());
        // ANNUL_QUARTERING
        user.json(post("/api/v1/shifts/" + shift + "/quartering/annul"), "{\"reason\":\"Báscula descalibrada.\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualityStatus").value("ANULADO"));
    }

    private long openShift(int day, String type) throws Exception {
        String body = teacher.json(post("/api/v1/studies/" + RUNNING_STUDY + "/shifts"),
                        "{\"dayNumber\":" + day + ",\"type\":\"" + type + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("id").asLong();
    }

    private void coverEverySource(long shift) throws Exception {
        List<Long> sources = sources(shift);
        teacher.json(put("/api/v1/shifts/" + shift + "/population"), "{\"students\":300,\"teachers\":12,\"staff\":6}")
                .andExpect(status().isOk());
        teacher.json(post("/api/v1/shifts/" + shift + "/bags"),
                        "{\"sourceId\":" + sources.get(0) + ",\"grossWeight\":12.40,\"tare\":0.40,\"volume\":0.080}")
                .andExpect(status().isCreated());
        for (Long source : sources.subList(1, sources.size())) {
            teacher.perform(post("/api/v1/shifts/" + shift + "/sources/" + source + "/no-generation"))
                    .andExpect(status().isOk());
        }
    }

    private List<Long> sources(long shift) throws Exception {
        String body = teacher.perform(get("/api/v1/shifts/" + shift)).andReturn().getResponse().getContentAsString();
        List<Long> ids = new ArrayList<>();
        json.readTree(body).path("sources").forEach(node -> ids.add(node.path("sourceId").asLong()));
        return ids;
    }

    private long firstPendingAlert() throws Exception {
        String body = teacher.perform(get("/api/v1/alerts").param("attended", "false")).andReturn().getResponse()
                .getContentAsString();
        JsonNode items = json.readTree(body).path("items");
        return items.get(0).path("id").asLong();
    }

    private String hazardousNonConformingQuartering() {
        Long white = jdbc.queryForObject("SELECT id FROM SUBCATEGORIA_RESIDUO WHERE grupo = 'BLANCO' ORDER BY id LIMIT 1",
                Long.class);
        Long hazardous = jdbc.queryForObject(
                "SELECT id FROM SUBCATEGORIA_RESIDUO WHERE grupo = 'SIN_COLOR' ORDER BY id LIMIT 1", Long.class);
        return "{\"sampleWeight\":50.00,\"results\":[{\"categoryId\":" + white + ",\"weight\":45.00},"
                + "{\"categoryId\":" + hazardous + ",\"weight\":0.40}]}";
    }

    private static String studyIn(int year) {
        StringBuilder days = new StringBuilder();
        for (int day = 1; day <= 8; day++) {
            days.append(day == 1 ? "" : ",")
                    .append("{\"dayNumber\":").append(day).append(",\"date\":\"").append(year)
                    .append("-02-").append(String.format("%02d", day + 1)).append("\"}");
        }
        return "{\"days\":[" + days + "]}";
    }
}
