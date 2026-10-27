package com.ecopoint.service;

import com.ecopoint.model.User;
import com.ecopoint.validation.UserValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Business service responsible for managing {@link User} registration and lookup.
 * Delegates all input format and email uniqueness checks to {@link UserValidator}
 * following SOLID SRP and GRASP Low Coupling.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class UserService {

    private final List<User> users;
    private int nextId;

    /**
     * Constructs a UserService with an empty in-memory user registry.
     */
    public UserService() {
        this.users = new ArrayList<>();
        this.nextId = 1;
    }

    /**
     * Registers a new user after validating name characters, email format,
     * institutional role, and email uniqueness.
     *
     * @param user user to register
     * @return registered user with its assigned identifier
     */
    public User registerUser(User user) {
        UserValidator.validateUserData(user);
        UserValidator.validateUniqueEmail(user.getEmail(), this.users);

        user.setIdUser(nextId++);
        user.setFirstName(user.getFirstName().trim());
        user.setLastName(user.getLastName().trim());
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setRole(user.getRole().trim());

        this.users.add(user);
        return user;
    }

    /**
     * Finds a user by unique identifier.
     *
     * @param idUser identifier of the user
     * @return matching user instance
     * @throws com.ecopoint.exceptions.ResourceNotFoundException if the user does not exist
     */
    public User getUserById(int idUser) {
        User found = null;
        for (User user : users) {
            if (user.getIdUser() == idUser) {
                found = user;
                break;
            }
        }
        UserValidator.validateUserExists(found, idUser);
        return found;
    }

    /**
     * Finds a user by email address.
     *
     * @param email email address to search
     * @return matching user instance
     * @throws com.ecopoint.exceptions.ResourceNotFoundException if no user matches the email
     */
    public User getUserByEmail(String email) {
        UserValidator.validateEmailFormat(email);
        String normalized = email.trim();
        User found = null;
        for (User user : users) {
            if (user.getEmail().equalsIgnoreCase(normalized)) {
                found = user;
                break;
            }
        }
        UserValidator.validateUserExists(found, normalized);
        return found;
    }

    /**
     * Returns an unmodifiable view of all registered users.
     *
     * @return list of registered users
     */
    public List<User> listAllUsers() {
        return Collections.unmodifiableList(users);
    }
}

