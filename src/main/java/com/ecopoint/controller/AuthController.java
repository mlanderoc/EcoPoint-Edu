package com.ecopoint.controller;

import com.ecopoint.dto.LoginRequest;
import com.ecopoint.dto.UserResponse;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.AuthService;
import com.ecopoint.service.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login, logout and current user (spec 003, section 4.1 of the plan).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository contextRepository;

    /**
     * Creates the controller.
     *
     * @param authService       login service
     * @param contextRepository keeps the authenticated user in the session
     */
    public AuthController(AuthService authService, SecurityContextRepository contextRepository) {
        this.authService = authService;
        this.contextRepository = contextRepository;
    }

    /**
     * Logs in and opens the session (R-120, CA-201, CA-202).
     *
     * @param login    email and password
     * @param request  current request
     * @param response current response, where the session cookie is set
     * @return the user with its institution and permissions
     */
    @PostMapping("/login")
    public UserResponse login(@RequestBody LoginRequest login, HttpServletRequest request,
                              HttpServletResponse response) {
        AuthenticatedUser user = authService.login(login.email(), login.password());
        startSession(user, request, response);
        return toResponse(user);
    }

    /**
     * Closes the session.
     *
     * @param request current request
     * @return 204 without body
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the user of the session (R-125).
     *
     * @param principal user kept in the session
     * @return the user with its institution and permissions
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal EcoPointUserDetails principal) {
        return toResponse(authService.currentUser(principal.getUser().getId()));
    }

    /**
     * Sends the {@code XSRF-TOKEN} cookie that the frontend returns in the
     * {@code X-XSRF-TOKEN} header (decision T3).
     *
     * @param token CSRF token of the request; reading it writes the cookie
     * @return 204 without body
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    private void startSession(AuthenticatedUser user, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        EcoPointUserDetails principal = new EcoPointUserDetails(user.user());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null,
                principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    private static UserResponse toResponse(AuthenticatedUser user) {
        return UserResponse.from(user.user(), user.institution());
    }
}
