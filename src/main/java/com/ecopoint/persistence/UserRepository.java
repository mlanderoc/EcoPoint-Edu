package com.ecopoint.persistence;

import com.ecopoint.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Storage contract for users (spec 002, R-050). The password hash is read and
 * written only through its own methods and never travels inside {@link User}
 * (spec 003, decision T5).
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

    /**
     * Returns the BCrypt hash of the password of the user with that email,
     * ignoring upper and lower case. Used only to authenticate (spec 003, R-120).
     *
     * @param email email address
     * @return the hash, or empty if the email does not exist or has no password yet
     */
    Optional<String> findPasswordHashByEmail(String email);

    /**
     * Replaces the BCrypt hash of the password of a user (spec 003, R-120).
     *
     * @param userId       user id
     * @param passwordHash new BCrypt hash, never the plain password
     * @throws com.ecopoint.exceptions.ResourceNotFoundException if the user does not exist
     */
    void updatePasswordHash(Long userId, String passwordHash);
}
