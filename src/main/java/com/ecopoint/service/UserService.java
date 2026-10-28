package com.ecopoint.service;

import com.ecopoint.model.User;
import com.ecopoint.persistence.FileUserRepository;
import com.ecopoint.persistence.UserRepository;
import com.ecopoint.validation.UserValidator;

import java.util.List;

/**
 * Business service responsible for managing User authentication and CRUD operations.
 * Depends on UserRepository applying Protected Variations and SOLID DIP.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class UserService {

    private final UserRepository userRepository;

    /**
     * Constructs a UserService using the default flat-file repository implementation.
     */
    public UserService() {
        this(new FileUserRepository());
    }

    /**
     * Constructs a UserService with an injected UserRepository contract.
     *
     * @param userRepository repository implementation to use
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authenticates a user by email address for session login.
     *
     * @param email institutional or personal email entered at login
     * @return authenticated User instance
     */
    public User authenticateByEmail(String email) {
        UserValidator.validateEmailFormat(email);
        User found = userRepository.findByEmail(email.trim());
        UserValidator.validateUserExists(found, email.trim());
        return found;
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
        UserValidator.validateUniqueEmail(user.getEmail(), userRepository.findAll());

        user.setIdUser(0);
        user.setFirstName(user.getFirstName().trim());
        user.setLastName(user.getLastName().trim());
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setRole(user.getRole().trim());

        return userRepository.save(user);
    }

    /**
     * Updates an existing user after verifying existence, format rules, and email uniqueness.
     *
     * @param user user instance containing updated fields and existing idUser
     * @return updated user instance
     */
    public User updateUser(User user) {
        UserValidator.validateUserData(user);
        getUserById(user.getIdUser());
        UserValidator.validateUniqueEmailForUpdate(
                user.getIdUser(), user.getEmail(), userRepository.findAll());

        user.setFirstName(user.getFirstName().trim());
        user.setLastName(user.getLastName().trim());
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setRole(user.getRole().trim());

        return userRepository.save(user);
    }

    /**
     * Deletes a user by identifier.
     *
     * @param idUser identifier of the user to delete
     */
    public void deleteUser(int idUser) {
        getUserById(idUser);
        userRepository.deleteById(idUser);
    }

    /**
     * Finds a user by unique identifier.
     *
     * @param idUser identifier of the user
     * @return matching user instance
     */
    public User getUserById(int idUser) {
        User found = userRepository.findById(idUser);
        UserValidator.validateUserExists(found, idUser);
        return found;
    }

    /**
     * Finds a user by email address.
     *
     * @param email email address to search
     * @return matching user instance
     */
    public User getUserByEmail(String email) {
        UserValidator.validateEmailFormat(email);
        String normalized = email.trim();
        User found = userRepository.findByEmail(normalized);
        UserValidator.validateUserExists(found, normalized);
        return found;
    }

    /**
     * Returns all registered users.
     *
     * @return list of registered users
     */
    public List<User> listAllUsers() {
        return userRepository.findAll();
    }
}
