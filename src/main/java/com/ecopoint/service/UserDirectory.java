package com.ecopoint.service;

import com.ecopoint.model.User;
import com.ecopoint.persistence.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Names of the users shown next to what they typed, verified or attended.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Component
public class UserDirectory {

    private final UserRepository users;

    /**
     * Creates the component.
     *
     * @param users user repository
     */
    public UserDirectory(UserRepository users) {
        this.users = users;
    }

    /**
     * Returns the full name of a user.
     *
     * @param id user id, or null
     * @return the full name, or null if there is no such user
     */
    public String nameOf(Long id) {
        return id == null ? null : users.findById(id).map(User::getFullName).orElse(null);
    }
}
