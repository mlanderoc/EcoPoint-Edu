package com.ecopoint.persistence;

import com.ecopoint.model.User;
import java.util.List;

/**
 * Persistence contract for User entities applying Protected Variations and DIP.
 * Allows transparent migration from flat files to a relational database.
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface UserRepository {

    /**
     * Saves a new user or updates an existing user in storage.
     *
     * @param user user instance to persist
     * @return persisted user with assigned identifier
     */
    User save(User user);

    /**
     * Finds a user by unique identifier.
     *
     * @param idUser user identifier
     * @return matching user or null if not found
     */
    User findById(int idUser);

    /**
     * Finds a user by email address.
     *
     * @param email email address to search
     * @return matching user or null if not found
     */
    User findByEmail(String email);

    /**
     * Returns all persisted users.
     *
     * @return list of all users
     */
    List<User> findAll();

    /**
     * Deletes a user by identifier.
     *
     * @param idUser identifier of the user to remove
     * @return true if the user was deleted, false otherwise
     */
    boolean deleteById(int idUser);
}
