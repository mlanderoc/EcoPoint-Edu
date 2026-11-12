package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration through the API against the demo data (spec 003, CA-203 and CA-210).
 *
 * @author craulgomez
 */
@AutoConfigureMockMvc
class AdministrationIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "EcoPoint2026";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void loadDemoData() {
        runScript("002_seed.sql");
    }

    @Test
    void ca210_passwordOfANewUserIsStoredOnlyAsABcryptHash() throws Exception {
        ApiClient teacher = ApiClient.loggedIn(mvc, "docente.demo@unicesar.edu.co", PASSWORD);

        teacher.json(post("/api/v1/users"), """
                        {"firstName":"Mario","lastName":"Gómez","email":"mario.gomez@unicesar.edu.co",
                         "role":"ESTUDIANTE_UNIVERSITARIO","password":"ClaveSegura2026"}""")
                .andExpect(status().isCreated());

        String hash = jdbc.queryForObject("SELECT clave_hash FROM USUARIO WHERE correo = 'mario.gomez@unicesar.edu.co'",
                String.class);
        assertTrue(hash.startsWith("$2a$10$"));
        assertFalse(hash.contains("ClaveSegura2026"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM USUARIO WHERE clave_hash LIKE '%ClaveSegura2026%'", Integer.class));

        ApiClient.anonymous(mvc).login("mario.gomez@unicesar.edu.co", "ClaveSegura2026").andExpect(status().isOk());
    }

    @Test
    void ca203_studentCannotCreateSourcesButCanListThem() throws Exception {
        ApiClient student = ApiClient.loggedIn(mvc, "estudiante.demo@unicesar.edu.co", PASSWORD);
        Long institution = jdbc.queryForObject("SELECT id FROM INSTITUCION", Long.class);

        student.json(post("/api/v1/institutions/" + institution + "/sources"),
                        "{\"code\":\"CAF\",\"name\":\"Cafetería\",\"type\":\"OTRA\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));

        student.perform(get("/api/v1/institutions/" + institution + "/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void ca002_teacherCreatesASourceAndARepeatedCodeIsRejected() throws Exception {
        ApiClient teacher = ApiClient.loggedIn(mvc, "docente.demo@unicesar.edu.co", PASSWORD);
        Long institution = jdbc.queryForObject("SELECT id FROM INSTITUCION", Long.class);
        String sources = "/api/v1/institutions/" + institution + "/sources";

        teacher.json(post(sources), "{\"code\":\"CAF\",\"name\":\"Cafetería\",\"type\":\"OTRA\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CAF"));

        teacher.json(post(sources), "{\"code\":\"CAF\",\"name\":\"Otra cafetería\",\"type\":\"OTRA\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("ERR_BUSINESS_RULE"));
    }

    @Test
    void rn005_sourceWithBagsCannotBeDeleted() throws Exception {
        ApiClient teacher = ApiClient.loggedIn(mvc, "docente.demo@unicesar.edu.co", PASSWORD);
        Long institution = jdbc.queryForObject("SELECT id FROM INSTITUCION", Long.class);
        Long store = jdbc.queryForObject("SELECT id FROM FUENTE_GENERACION WHERE codigo = 'TI'", Long.class);

        teacher.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/institutions/" + institution + "/sources/" + store))
                .andExpect(status().isUnprocessableEntity());

        teacher.perform(post("/api/v1/institutions/" + institution + "/sources/" + store + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
