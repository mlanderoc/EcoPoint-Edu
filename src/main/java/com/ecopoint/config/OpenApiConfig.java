package com.ecopoint.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Description of the API for the OpenAPI document and Swagger UI, available
 * only with the {@code dev} profile (spec 003, R-130 and decision T13).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Configuration
public class OpenApiConfig {

    private static final String DESCRIPTION = """
            API REST de EcoPoint Edu: caracterización de residuos sólidos en instituciones educativas.

            Sesión: inicie sesión con POST /api/v1/auth/login. El servidor guarda la sesión en la cookie \
            HttpOnly JSESSIONID durante 8 horas.

            CSRF: llame primero GET /api/v1/auth/csrf; envíe el valor de la cookie XSRF-TOKEN en la cabecera \
            X-XSRF-TOKEN en cada POST, PUT, PATCH y DELETE.

            Errores: todos responden con timestamp, status, errorCode, message, path y fieldErrors, \
            con el mensaje en español.""";

    /**
     * General information of the API.
     *
     * @return the OpenAPI description
     */
    @Bean
    public OpenAPI ecoPointOpenApi() {
        return new OpenAPI().info(new Info()
                .title("EcoPoint Edu API")
                .version("v1")
                .description(DESCRIPTION)
                .license(new License().name("Proyecto de aula — Universidad Popular del Cesar")));
    }
}
