package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.persistence.UserRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.UserValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Administration of users: only a teacher creates them; there is no open sign-up
 * (spec 003, P4 and section 5.1; spec 004, R-322).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates the service.
     *
     * @param users           user repository
     * @param passwordEncoder BCrypt password encoder
     */
    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Lists the users of the institution of the actor, with optional filters.
     *
     * @param actor  user that asks
     * @param role   role to keep, or null for all
     * @param active true or false to filter by state, or null for all
     * @return users ordered by last name and first name
     */
    public List<User> list(User actor, UserRole role, Boolean active) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        return users.findByInstitution(actor.getInstitutionId()).stream()
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> active == null || user.isActive() == active)
                .toList();
    }

    /**
     * Finds a user.
     *
     * @param actor user that asks
     * @param id    user id
     * @return the user
     */
    public User get(User actor, Long id) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        return find(id);
    }

    /**
     * Creates a user with its initial password, stored only as a BCrypt hash
     * (R-120, RN-084).
     *
     * @param actor    teacher that creates the user
     * @param user     data of the new user; without institution it joins the one of the actor
     * @param password initial password
     * @return the saved user
     */
    @Transactional
    public User create(User actor, User user, String password) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        user.setId(null);
        if (user.getInstitutionId() == null) {
            user.setInstitutionId(actor.getInstitutionId());
        }
        UserValidator.validate(user);
        UserValidator.validateUniqueEmail(user, users.findEmails());
        UserValidator.validatePassword(password);
        User saved = users.save(user);
        users.updatePasswordHash(saved.getId(), passwordEncoder.encode(password));
        return saved;
    }

    /**
     * Updates the data of a user and, if a new password is given, replaces it.
     *
     * @param actor       teacher that edits
     * @param id          user id
     * @param changes     new data
     * @param newPassword new password, or null to keep the current one
     * @return the saved user
     */
    @Transactional
    public User update(User actor, Long id, User changes, String newPassword) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        User user = find(id);
        Set<String> otherEmails = new HashSet<>(users.findEmails());
        otherEmails.removeIf(email -> User.normalizeEmail(email).equals(User.normalizeEmail(user.getEmail())));
        user.setFirstName(changes.getFirstName());
        user.setLastName(changes.getLastName());
        user.setEmail(changes.getEmail());
        user.setRole(changes.getRole());
        UserValidator.validate(user);
        UserValidator.validateUniqueEmail(user, otherEmails);
        if (newPassword != null) {
            UserValidator.validatePassword(newPassword);
        }
        User saved = users.save(user);
        if (newPassword != null) {
            users.updatePasswordHash(id, passwordEncoder.encode(newPassword));
        }
        return saved;
    }

    /**
     * Deactivates a user: it can no longer log in, but what it registered is kept.
     *
     * @param actor teacher that acts
     * @param id    user id
     * @return the user, now inactive
     */
    public User deactivate(User actor, Long id) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        User user = find(id);
        users.deactivate(id);
        user.deactivate();
        return user;
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }
}
