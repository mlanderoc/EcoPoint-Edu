package com.ecopoint.api;

import com.ecopoint.model.User;
import com.ecopoint.persistence.PostgresIntegrationTest;
import com.ecopoint.service.PasswordResetMailer;
import com.ecopoint.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Password recovery by email against the demo data (spec D18, CP-01 to CP-09).
 * The mailer is replaced, so the test reads the code that would go in the email.
 *
 * @author craulgomez
 */
@AutoConfigureMockMvc
class PasswordResetIT extends PostgresIntegrationTest {

    private static final String STUDENT = "estudiante.demo@unicesar.edu.co";
    private static final String OLD_PASSWORD = "EcoPoint2026";
    private static final String NEW_PASSWORD = "ClaveNueva2026";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PasswordResetMailer mailer;

    @BeforeEach
    void loadDemoData() {
        runScript("002_seed.sql");
    }

    @Test
    void cp01_registeredEmailReceivesALink() throws Exception {
        requestLink(STUDENT).andExpect(status().isNoContent());

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(mailer).sendResetLink(user.capture(), anyString());
        assertEquals(STUDENT, user.getValue().getEmail());
    }

    @Test
    void cp02_unknownAndInactiveEmailsGetTheSameAnswerWithoutEmail() throws Exception {
        jdbc.update("UPDATE USUARIO SET activo = FALSE WHERE correo = 'sofia.torres@unicesar.edu.co'");

        requestLink("nadie@unicesar.edu.co").andExpect(status().isNoContent());
        requestLink("sofia.torres@unicesar.edu.co").andExpect(status().isNoContent());

        verify(mailer, never()).sendResetLink(any(), anyString());
    }

    @Test
    void cp03_newPasswordWorksAndTheOldOneDoesNot() throws Exception {
        String code = linkFor(STUDENT);

        confirm(code, NEW_PASSWORD).andExpect(status().isNoContent());

        ApiClient.anonymous(mvc).login(STUDENT, NEW_PASSWORD).andExpect(status().isOk());
        ApiClient.anonymous(mvc).login(STUDENT, OLD_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void cp04_usedOrInventedLinkIsRejected() throws Exception {
        String code = linkFor(STUDENT);
        confirm(code, NEW_PASSWORD).andExpect(status().isNoContent());

        confirm(code, "OtraClave2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PasswordResetService.INVALID_LINK));
        confirm("codigo-inventado", "OtraClave2026").andExpect(status().isBadRequest());
    }

    @Test
    void cp04_expiredLinkIsRejected() throws Exception {
        String code = linkFor(STUDENT);
        jdbc.update("UPDATE TOKEN_RECUPERACION SET creado_en = creado_en - INTERVAL '1 hour', "
                + "vence_en = vence_en - INTERVAL '1 hour'");

        confirm(code, NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void cp05_secondLinkCancelsTheFirst() throws Exception {
        String first = linkFor(STUDENT);
        clearInvocations(mailer);
        String second = linkFor(STUDENT);

        confirm(first, NEW_PASSWORD).andExpect(status().isBadRequest());
        confirm(second, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void cp06_changingThePasswordClosesTheOpenSessions() throws Exception {
        ApiClient open = ApiClient.loggedIn(mvc, STUDENT, OLD_PASSWORD);
        open.perform(get("/api/v1/auth/me")).andExpect(status().isOk());

        confirm(linkFor(STUDENT), NEW_PASSWORD).andExpect(status().isNoContent());

        open.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void cp07_fourthLinkInAnHourIsNotSent() throws Exception {
        for (int i = 0; i < 4; i++) {
            requestLink(STUDENT).andExpect(status().isNoContent());
        }

        verify(mailer, times(PasswordResetService.MAX_LINKS_PER_HOUR)).sendResetLink(any(), anyString());
    }

    @Test
    void cp08_invalidPasswordIsRejectedAndTheLinkStillWorks() throws Exception {
        String code = linkFor(STUDENT);

        confirm(code, "corta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));
        confirm(code, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void cp09_databaseKeepsOnlyTheHashOfTheCode() throws Exception {
        String code = linkFor(STUDENT);

        List<String> stored = jdbc.queryForList("SELECT codigo_hash FROM TOKEN_RECUPERACION", String.class);
        assertEquals(1, stored.size());
        assertNotEquals(code, stored.get(0));
        assertEquals(PasswordResetService.hash(code), stored.get(0));
    }

    @Test
    void p7_requestWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + STUDENT + "\"}"))
                .andExpect(status().isForbidden());
    }

    private String linkFor(String email) throws Exception {
        requestLink(email).andExpect(status().isNoContent());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendResetLink(any(), code.capture());
        return code.getValue();
    }

    private ResultActions requestLink(String email) throws Exception {
        return ApiClient.anonymous(mvc).perform(post("/api/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions confirm(String code, String password) throws Exception {
        return ApiClient.anonymous(mvc).perform(post("/api/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + code + "\",\"newPassword\":\"" + password + "\"}"));
    }
}
