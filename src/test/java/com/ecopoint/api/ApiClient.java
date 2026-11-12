package com.ecopoint.api;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Calls the API the way the frontend does: it asks for the CSRF cookie, sends it
 * back in the {@code X-XSRF-TOKEN} header and keeps the session cookie (spec 003,
 * decisions T1 and T3). The integration tests use it instead of the
 * {@code csrf()} helper of Spring Security Test, which replaces the token
 * repository of the shared application context.
 *
 * @author craulgomez
 */
final class ApiClient {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private final MockMvc mvc;
    private final String csrfToken;
    private MockHttpSession session;

    private ApiClient(MockMvc mvc, String csrfToken) {
        this.mvc = mvc;
        this.csrfToken = csrfToken;
    }

    /**
     * Starts a client without session, holding a fresh CSRF token.
     *
     * @param mvc MockMvc of the application
     * @return the client
     * @throws Exception if the request fails
     */
    static ApiClient anonymous(MockMvc mvc) throws Exception {
        Cookie cookie = mvc.perform(get("/api/v1/auth/csrf")).andReturn().getResponse().getCookie(CSRF_COOKIE);
        if (cookie == null) {
            throw new IllegalStateException("La API no entregó la cookie " + CSRF_COOKIE);
        }
        return new ApiClient(mvc, cookie.getValue());
    }

    /**
     * Starts a client and logs in.
     *
     * @param mvc      MockMvc of the application
     * @param email    email
     * @param password password
     * @return the client with its session
     * @throws Exception if the login fails
     */
    static ApiClient loggedIn(MockMvc mvc, String email, String password) throws Exception {
        ApiClient client = anonymous(mvc);
        MvcResult result = client.login(email, password).andReturn();
        if (result.getResponse().getStatus() != 200) {
            throw new IllegalStateException("No se pudo iniciar sesión con " + email);
        }
        return client;
    }

    /**
     * Sends the login request and keeps the session it opens.
     *
     * @param email    email
     * @param password password
     * @return the result to check
     * @throws Exception if the request fails
     */
    ResultActions login(String email, String password) throws Exception {
        ResultActions result = perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
        MockHttpSession opened = (MockHttpSession) result.andReturn().getRequest().getSession(false);
        if (opened != null) {
            session = opened;
        }
        return result;
    }

    /**
     * Sends a request with the CSRF cookie and header and, if any, the session.
     *
     * @param request request to send
     * @return the result to check
     * @throws Exception if the request fails
     */
    ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        request.cookie(new Cookie(CSRF_COOKIE, csrfToken)).header(CSRF_HEADER, csrfToken);
        if (session != null) {
            request.session(session);
        }
        return mvc.perform(request);
    }

    /**
     * Sends a JSON body.
     *
     * @param request request to send
     * @param json    body
     * @return the result to check
     * @throws Exception if the request fails
     */
    ResultActions json(MockHttpServletRequestBuilder request, String json) throws Exception {
        return perform(request.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /**
     * Returns the session of the client, or null before the login.
     *
     * @return the session
     */
    MockHttpSession session() {
        return session;
    }
}
