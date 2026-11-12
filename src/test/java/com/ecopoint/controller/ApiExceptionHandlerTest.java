package com.ecopoint.controller;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.exceptions.QualityControlException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Error format of the API (spec 003, R-110 to R-112): one case per row of the
 * table in section 4.2 of the plan.
 *
 * @author craulgomez
 */
@WebMvcTest(controllers = ApiExceptionHandlerTest.FailingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApiExceptionHandlerTest.FailingController.class, ApiExceptionHandlerTest.FixedClock.class})
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void ca205_invalidFieldIsReportedWithItsJsonName() throws Exception {
        expectError(get("/test/invalid-tare"), 400, "ERR_INVALID_DATA")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("tare"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("La tara no puede superar el peso bruto."))
                .andExpect(jsonPath("$.message").value(
                        "Dato inválido en el campo 'tara': La tara no puede superar el peso bruto."));
    }

    @Test
    void ca205_fieldWithoutTranslationGoesOnlyInTheMessage() throws Exception {
        expectError(get("/test/invalid-general"), 400, "ERR_INVALID_DATA")
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(jsonPath("$.message").value("El archivo no es válido."));
    }

    @Test
    void r110_malformedJsonIsInvalidData() throws Exception {
        expectError(post("/test/echo").contentType(MediaType.APPLICATION_JSON).content("{ no es json"),
                400, "ERR_INVALID_DATA");
    }

    @Test
    void r110_parameterOfTheWrongTypeIsInvalidData() throws Exception {
        expectError(get("/test/number/abc"), 400, "ERR_INVALID_DATA");
    }

    @Test
    void ca202_failedLoginIsUnauthorized() throws Exception {
        expectError(get("/test/unauthorized"), 401, "ERR_UNAUTHORIZED")
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos."));
    }

    @Test
    void ca203_accessDeniedIsForbidden() throws Exception {
        expectError(get("/test/forbidden"), 403, "ERR_FORBIDDEN")
                .andExpect(jsonPath("$.message").value("No tiene permiso para crear estudios."));
    }

    @Test
    void ca207_missingRecordIsNotFound() throws Exception {
        expectError(get("/test/missing"), 404, "ERR_NOT_FOUND");
    }

    @Test
    void r111_unknownPathIsNotFound() throws Exception {
        expectError(get("/test/does-not-exist"), 404, "ERR_NOT_FOUND");
    }

    @Test
    void r111_unsupportedMethodIsReported() throws Exception {
        expectError(put("/test/missing"), 405, "ERR_INVALID_DATA");
    }

    @Test
    void ca206_businessRuleIsUnprocessable() throws Exception {
        expectError(get("/test/business"), 422, "ERR_BUSINESS_RULE")
                .andExpect(jsonPath("$.message").value("Registre primero la población del turno."));
    }

    @Test
    void r111_qualityControlKeepsItsOwnCode() throws Exception {
        expectError(get("/test/quality"), 422, "ERR_QUALITY_CONTROL");
    }

    @Test
    void ca208_databaseFailureHidesSqlAndStackTrace() throws Exception {
        expectError(get("/test/database"), 500, "ERR_PERSISTENCE")
                .andExpect(jsonPath("$.message").value(ApiExceptionHandler.INTERNAL_MESSAGE))
                .andExpect(content().string(not(containsString("SELECT"))))
                .andExpect(content().string(not(containsString("SQLException"))))
                .andExpect(content().string(not(containsString("at com.ecopoint"))));
    }

    @Test
    void ca208_unexpectedErrorIsInternalWithoutDetails() throws Exception {
        expectError(get("/test/unexpected"), 500, "ERR_INTERNAL")
                .andExpect(jsonPath("$.message").value(ApiExceptionHandler.INTERNAL_MESSAGE))
                .andExpect(content().string(not(containsString("NullPointerException"))));
    }

    private ResultActions expectError(org.springframework.test.web.servlet.RequestBuilder request, int status,
                                      String errorCode) throws Exception {
        return mvc.perform(request)
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.errorCode").value(errorCode))
                .andExpect(jsonPath("$.timestamp").value("2026-11-12T07:30:00"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    /** Controller that throws each kind of error. */
    @RestController
    static class FailingController implements FieldNameMapping {

        @Override
        public Map<String, String> fieldNames() {
            return Map.of("tara", "tare");
        }

        @GetMapping("/test/invalid-tare")
        void invalidTare() {
            throw new InvalidDataException("tara", "La tara no puede superar el peso bruto.");
        }

        @GetMapping("/test/invalid-general")
        void invalidGeneral() {
            throw new InvalidDataException("El archivo no es válido.");
        }

        @PostMapping("/test/echo")
        Map<String, Object> echo(@RequestBody Map<String, Object> body) {
            return body;
        }

        @GetMapping("/test/number/{value}")
        int number(@PathVariable int value) {
            return value;
        }

        @GetMapping("/test/unauthorized")
        void unauthorized() {
            throw new UnauthorizedException("Correo o contraseña incorrectos.");
        }

        @GetMapping("/test/forbidden")
        void forbidden() {
            throw new AccessDeniedException("No tiene permiso para crear estudios.");
        }

        @GetMapping("/test/missing")
        void missing() {
            throw new ResourceNotFoundException("Estudio", 999);
        }

        @GetMapping("/test/business")
        void business() {
            throw new BusinessRuleException("Registre primero la población del turno.");
        }

        @GetMapping("/test/quality")
        void quality() {
            throw new QualityControlException("El cuarteo supera la tolerancia del 2 %.");
        }

        @GetMapping("/test/database")
        void database() {
            throw new PersistenceException("consultar el estudio",
                    new SQLException("ERROR: relation \"ESTUDIO\" does not exist in SELECT * FROM ESTUDIO"));
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new NullPointerException("valor nulo inesperado");
        }
    }

    /** Fixed clock so the timestamp of the errors is predictable. */
    @TestConfiguration
    static class FixedClock {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-12T12:30:00Z"), ZoneId.of("America/Bogota"));
        }
    }
}
