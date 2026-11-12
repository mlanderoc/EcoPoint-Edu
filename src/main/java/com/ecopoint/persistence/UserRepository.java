package com.ecopoint.persistence;

import com.ecopoint.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Storage contract for users (spec 002, R-050). Password hashes are handled by
 * the security layer of spec 003.
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface UserRepository {

    /**
     * Inserts a new user (id null) or updates an existing one. Updating never
     * changes the stored password hash.
     *
     * @param user user to save
     * @return the same user, with its id assigned
     */
    User save(User user);

    /**
     * Finds a user by id.
     *
     * @param id user id
     * @return the user, or empty if it does not exist
     */
    Optional<User> findById(Long id);

    /**
     * Finds a user by email, ignoring upper and lower case (RN-084).
     *
     * @param email email address
     * @return the user, or empty if it does not exist
     */
    Optional<User> findByEmail(String email);

    /**
     * Lists all users ordered by last name and first name.
     *
     * @return users, active and inactive
     */
    List<User> findAll();

    /**
     * Lists the users of an institution.
     *
     * @param institutionId institution id
     * @return users of the institution
     */
    List<User> findByInstitution(Long institutionId);

    /**
     * Returns the emails already registered, for the uniqueness rule (RN-084).
     *
     * @return set of emails
     */
    Set<String> findEmails();

    /**
     * Deactivates a user; it keeps the history of what it registered.
     *
     * @param userId user id
     */
    void deactivate(Long userId);
}
