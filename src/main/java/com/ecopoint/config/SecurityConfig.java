package com.ecopoint.config;

import com.ecopoint.security.JsonAccessDeniedHandler;
import com.ecopoint.security.JsonAuthenticationEntryPoint;
import com.ecopoint.security.SpaCsrfTokenRequestHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security of the API (spec 003): session cookie, CSRF, CORS and the public
 * routes. The login itself is checked by {@code AuthService}, which then keeps the
 * user in the session; the permissions of RN-081 are checked by the services with
 * {@code AccessPolicy} (decisions T1 to T5).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Configuration
public class SecurityConfig {

    /** Cost factor of BCrypt (decision T5). */
    private static final int BCRYPT_STRENGTH = 10;

    /**
     * Filter chain of the API.
     *
     * @param http              security builder
     * @param entryPoint        401 answer without session
     * @param accessDenied      403 answer from the filters (CSRF)
     * @param contextRepository where the authenticated user is kept between requests
     * @return the filter chain
     * @throws Exception if the configuration is invalid
     */
    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http, JsonAuthenticationEntryPoint entryPoint,
                                           JsonAccessDeniedHandler accessDenied,
                                           SecurityContextRepository contextRepository) throws Exception {
        return http
                .cors(cors -> { })
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .authorizeHttpRequests(routes -> routes
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDenied))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .build();
    }

    /**
     * Keeps the authenticated user in the HTTP session (decision T1).
     *
     * @return the session-backed repository
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Password hashing with BCrypt (R-120, decision T5).
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    /**
     * Only the frontend origin may call the API with the session cookie; never
     * {@code *} (R-127, decision T4).
     *
     * @param frontendOrigin origin of the frontend, from {@code ECOPOINT_FRONTEND_ORIGIN}
     * @return the CORS configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${ecopoint.frontend-origin}") String frontendOrigin) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(frontendOrigin));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
