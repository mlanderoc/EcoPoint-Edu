package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login, session and logout against the demo data of the database (spec 003,
 * CA-200, CA-201, CA-202 and R-126).
 *
 * @author craulgomez
 */
@AutoConfigureMockMvc
class AuthIT extends PostgresIntegrationTest {

    private static final String TEACHER = "docente.demo@unicesar.edu.co";
    private static final String STUDENT = "estudiante.demo@unicesar.edu.co";
    private static final String PASSWORD = "EcoPoint2026";
    private static final String WRONG_LOGIN = "Correo o contraseña incorrectos.";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void loadDemoData() {
        runScript("002_seed.sql");
    }

    @Test
    void ca200_requestWithoutSessionGetsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/studies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR_UNAUTHORIZED"));
    }

    @Test
    void ca201_teacherLogsInAndTheSessionKnowsWhoItIs() throws Exception {
        ApiClient client = ApiClient.anonymous(mvc);
        client.login(TEACHER, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEACHER))
                .andExpect(jsonPath("$.role").value("DOCENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        client.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Docente"))
                .andExpect(jsonPath("$.lastName").value("Demo"))
                .andExpect(jsonPath("$.role").value("DOCENTE"))
                .andExpect(jsonPath("$.institutionName").value("I.E. Prudencia Daza"))
                .andExpect(jsonPath("$.permissions", hasItem("MANAGE_STUDIES")));
    }

    @Test
    void ca201_studentGetsOnlyThePermissionsOfItsRole() throws Exception {
        ApiClient.anonymous(mvc).login(STUDENT, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ESTUDIANTE_UNIVERSITARIO"))
                .andExpect(jsonPath("$.permissions", hasItem("REGISTER_BAGS")))
                .andExpect(jsonPath("$.permissions", not(hasItem("MANAGE_STUDIES"))));
    }

    @Test
    void ca201_emailIsAcceptedIgnoringCase() throws Exception {
        ApiClient.anonymous(mvc).login(" Docente.Demo@UNICESAR.edu.co", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void ca202_wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        ApiClient client = ApiClient.anonymous(mvc);
        client.login(TEACHER, "otra-clave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(WRONG_LOGIN));
        client.login("nadie@unicesar.edu.co", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(WRONG_LOGIN));
    }

    @Test
    void r126_fiveFailuresLockTheEmail() throws Exception {
        ApiClient client = ApiClient.anonymous(mvc);
        String email = "intentos.bloqueo@unicesar.edu.co";
        for (int i = 0; i < 5; i++) {
            client.login(email, "clave-errada").andExpect(status().isUnauthorized());
        }

        client.login(email, "clave-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Cuenta bloqueada temporalmente. Intente en 15 minutos."));
    }

    @Test
    void r121_logoutEndsTheSession() throws Exception {
        ApiClient client = ApiClient.loggedIn(mvc, TEACHER, PASSWORD);

        client.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());

        client.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void t3_csrfRouteSendsTheTokenCookieWithoutSession() throws Exception {
        mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("XSRF-TOKEN=")));
    }

    @Test
    void t3_loginWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEACHER + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"));
    }

    @Test
    void r130_apiDocumentationIsOffOutsideTheDevProfile() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }
}
