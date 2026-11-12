package com.ecopoint.service;

import com.ecopoint.model.Institution;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;

import java.util.Set;

/**
 * User of the current session together with its institution, so the frontend can
 * build its menu (spec 003, R-125).
 *
 * @param user        authenticated user
 * @param institution institution of the user
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record AuthenticatedUser(User user, Institution institution) {

    /**
     * Returns the permissions of the role of the user (RN-081).
     *
     * @return permissions of the role
     */
    public Set<Permission> permissions() {
        return user.getRole().getPermissions();
    }
}
