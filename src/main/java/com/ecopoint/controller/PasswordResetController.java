package com.ecopoint.controller;

import com.ecopoint.dto.PasswordResetConfirmRequest;
import com.ecopoint.dto.PasswordResetRequest;
import com.ecopoint.model.User;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.security.SessionControl;
import com.ecopoint.service.PasswordResetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Password recovery by email, without session (spec D18).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/auth/password-reset")
public class PasswordResetController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of("contrasena", "newPassword");

    private final PasswordResetService resets;
    private final SessionControl sessions;

    /**
     * Creates the controller.
     *
     * @param resets   password recovery service
     * @param sessions registry of the open sessions
     */
    public PasswordResetController(PasswordResetService resets, SessionControl sessions) {
        this.resets = resets;
        this.sessions = sessions;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Asks for a recovery link. The answer is always 204, whether the email
     * has an account or not (RP-01).
     *
     * @param request email of the account
     */
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void request(@RequestBody PasswordResetRequest request) {
        resets.requestReset(request.email());
    }

    /**
     * Saves the new password and closes every session of the user (RP-06, RP-07).
     *
     * @param request code of the link and new password
     */
    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@RequestBody PasswordResetConfirmRequest request) {
        User user = resets.confirmReset(request.token(), request.newPassword());
        sessions.closeAllSessions(new EcoPointUserDetails(user));
    }
}
