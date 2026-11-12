package com.ecopoint.model;

import java.util.Locale;

/**
 * Person who uses the system: a teacher (DOCENTE) or a university student
 * (ESTUDIANTE_UNIVERSITARIO). The role decides what the user may do (RN-080,
 * RN-081). Passwords and login belong to the security layer (spec 003).
 *
 * @author EstefaniaMarquez
 * @version 2.0
 */
public class User {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
    private Long institutionId;
    private boolean active;

    /**
     * Creates an empty, active user.
     */
    public User() {
        this.active = true;
    }

    /**
     * Creates an active user.
     *
     * @param id            identifier, or null if not saved yet
     * @param firstName     first name
     * @param lastName      last name
     * @param email         email address, unique in the system (RN-084)
     * @param role          user role
     * @param institutionId institution the user works with
     */
    public User(Long id, String firstName, String lastName, String email, UserRole role, Long institutionId) {
        this();
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.institutionId = institutionId;
    }

    /**
     * Normalizes an email address to compare it with others: trims spaces and
     * ignores upper and lower case (RN-084).
     *
     * @param email email to normalize
     * @return normalized email, or an empty string if the email is null
     */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the full name of the user.
     *
     * @return first name and last name separated by a space, without extra spaces
     *         when one of them is missing
     */
    public String getFullName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }

    /**
     * Indicates whether the user may perform an action: the user must be
     * active and the role must hold the permission (RN-081).
     *
     * @param permission permission to check
     * @return true if the active user's role grants the permission
     */
    public boolean hasPermission(Permission permission) {
        return active && role != null && role.hasPermission(permission);
    }

    /**
     * Deactivates the user; a deactivated user keeps the history of what it
     * registered but can no longer act.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Activates the user again.
     */
    public void activate() {
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public boolean isActive() {
        return active;
    }
}
