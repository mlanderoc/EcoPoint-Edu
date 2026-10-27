package com.ecopoint.model;

/**
 * Represents a person registered in the EcoPoint Edu system who participates
 * in field inspections or institutional environmental monitoring.
 * 
 * The {@code role} attribute is purely descriptive to indicate the person's
 * institutional position (e.g., Estudiante, Docente, Inspector Ambiental, Coordinador)
 * and does not enforce system access permissions.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */

public class User {

    private int idUser;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    /**
     * Default constructor.
     */
    public User() {
    }

    /**
     * Constructs a User with all attributes.
     *
     * @param idUser    unique identifier of the user
     * @param firstName user's first name(s)
     * @param lastName  user's last name(s)
     * @param email     unique institutional or personal email address
     * @param role      descriptive institutional role (e.g., Estudiante, Docente, Inspector)
     */
    public User(int idUser, String firstName, String lastName, String email, String role) {
        this.idUser = idUser;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }

    /**
     * Returns the full formatted name of the user (GRASP Information Expert).
     *
     * @return concatenated first name and last name
     */
    public String getFullName() {
        String first = (firstName != null) ? firstName.trim() : "";
        String last = (lastName != null) ? lastName.trim() : "";
        return (first + " " + last).trim();
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
