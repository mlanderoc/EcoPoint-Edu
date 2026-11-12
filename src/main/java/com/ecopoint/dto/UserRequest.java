package com.ecopoint.dto;

import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;

/**
 * Data to create or edit a user (spec 004, R-322).
 *
 * @param firstName first name
 * @param lastName  last name
 * @param email     email, unique (RN-084)
 * @param role      role (RN-080)
 * @param password  password: required to create, optional to edit
 * @author craulgomez
 * @version 1.0
 */
public record UserRequest(String firstName, String lastName, String email, UserRole role, String password) {

    /**
     * Builds the user of the domain, without id or institution.
     *
     * @return the user
     */
    public User toModel() {
        return new User(null, firstName, lastName, email, role, null);
    }
}
