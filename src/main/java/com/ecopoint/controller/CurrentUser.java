package com.ecopoint.controller;

import com.ecopoint.exceptions.UnauthorizedException;
import com.ecopoint.model.User;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.AuthService;
import org.springframework.stereotype.Component;

/**
 * Gives the controllers the user of the session, reloaded from the database on
 * every request so a deactivation or a role change takes effect at once
 * (spec 003, decision T11).
 *
 * @author craulgomez
 * @version 1.0
 */
@Component
public class CurrentUser {

    private final AuthService authService;

    /**
     * Creates the component.
     *
     * @param authService login service
     */
    public CurrentUser(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Returns the current user.
     *
     * @param principal user kept in the session
     * @return the user, as it is now in the database
     * @throws UnauthorizedException if there is no valid session
     */
    public User of(EcoPointUserDetails principal) {
        if (principal == null) {
            throw new UnauthorizedException(AuthService.SESSION_INVALID);
        }
        return authService.currentUser(principal.getUser().getId()).user();
    }
}
