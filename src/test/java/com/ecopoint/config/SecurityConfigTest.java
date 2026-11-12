package com.ecopoint.config;

import com.ecopoint.persistence.UserRepository;
import com.ecopoint.security.EcoPointUserDetailsService;
import com.ecopoint.security.ErrorResponseWriter;
import com.ecopoint.security.JsonAccessDeniedHandler;
import com.ecopoint.security.JsonAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security filters of the API (spec 003, T05): session required, CSRF, CORS and
 * JSON answers for 401 and 403.
 *
 * @author mlanderoc
 */
@WebMvcTest(controllers = SecurityConfigTest.ProtectedController.class)
@Import({SecurityConfig.class, ClockConfig.class, ErrorResponseWriter.class, JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class, EcoPointUserDetailsService.class, SecurityConfigTest.ProtectedController.class})
@TestPropertySource(properties = "ecopoint.frontend-origin=http://localhost:5173")
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository users;

    @Test
    void ca200_requestWithoutSessionGetsUnauthorizedJson() throws Exception {
        mvc.perform(get("/api/v1/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("ERR_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(JsonAuthenticationEntryPoint.MESSAGE))
                .andExpect(jsonPath("$.path").value("/api/v1/protected"));
    }

    @Test
    @WithMockUser
    void r121_requestWithSessionReachesTheController() throws Exception {
        mvc.perform(get("/api/v1/protected")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void t3_postWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/v1/protected"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(JsonAccessDeniedHandler.CSRF_MESSAGE));
    }

    @Test
    @WithMockUser
    void t3_postWithCsrfTokenIsAccepted() throws Exception {
        mvc.perform(post("/api/v1/protected").with(csrf())).andExpect(status().isOk());
    }

    @Test
    void r121_csrfRouteIsPublicAndSendsTheCookie() throws Exception {
        mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void r127_corsAcceptsOnlyTheFrontendOriginWithCredentials() throws Exception {
        mvc.perform(options("/api/v1/protected")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mvc.perform(options("/api/v1/protected")
                        .header("Origin", "http://sitio-ajeno.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void ca210_passwordsAreHashedWithBcrypt() {
        String hash = passwordEncoder.encode("EcoPoint2026");

        assertTrue(hash.startsWith("$2a$10$"));
        assertNotEquals("EcoPoint2026", hash);
        assertTrue(passwordEncoder.matches("EcoPoint2026",
                "$2a$10$MhdpayjBM9C3p0KGJ8zzwuMd.EZcmQKyTk9hnnaqxv04Vdhwal.Um"),
                "El hash de la semilla (spec 002) corresponde a la contraseña de demostración");
    }

    /** Stand-in for the real routes until their controllers exist. */
    @RestController
    static class ProtectedController {

        @GetMapping("/api/v1/protected")
        String read() {
            return "ok";
        }

        @PostMapping("/api/v1/protected")
        String write() {
            return "ok";
        }

        @GetMapping("/api/v1/auth/csrf")
        org.springframework.http.ResponseEntity<Void> csrfToken(org.springframework.security.web.csrf.CsrfToken token) {
            token.getToken();
            return org.springframework.http.ResponseEntity.noContent().build();
        }
    }
}
