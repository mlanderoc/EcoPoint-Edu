package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.User;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Stateless validator for {@link User} data and domain constraints
 * (GRASP Pure Fabrication &amp; SOLID Single Responsibility Principle).
 * <p>
 * Centralizes all user input format checks (no numbers in names, valid email
 * structure with {@code @}, valid descriptive institutional role) and uniqueness
 * verification without coupling the domain model to validation logic.
 * </p>
 *
 * @author Carlos Raul Gomez Galvis
 * @version 1.0
 */
public final class UserValidator {

    private static final Pattern LETTERS_AND_SPACES_PATTERN =
            Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+(\\s+[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+)*$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private UserValidator() {
    }

    /**
     * Validates all attributes of a {@link User} before registration or update.
     *
     * @param user user instance to validate
     * @throws InvalidDataException if any attribute is null, blank, or fails format rules
     */
    public static void validateUserData(User user) {
        if (user == null) {
            throw new InvalidDataException("usuario", "El objeto de usuario no puede ser nulo.");
        }
        validatePersonName(user.getFirstName(), "nombre");
        validatePersonName(user.getLastName(), "apellido");
        validateEmailFormat(user.getEmail());
        validateInstitutionalRole(user.getRole());
    }

    /**
     * Validates that a first name or last name is non-empty, has at least 2 characters,
     * and contains exclusively letters and spaces (no numbers or special characters).
     *
     * @param nameValue value to validate
     * @param fieldName Spanish field name for user-facing error messages
     * @throws InvalidDataException if the name is blank, too short, or contains numbers/symbols
     */
    public static void validatePersonName(String nameValue, String fieldName) {
        if (nameValue == null || nameValue.trim().isEmpty()) {
            throw new InvalidDataException(fieldName, "El " + fieldName + " del usuario es obligatorio.");
        }
        String trimmed = nameValue.trim();
        if (trimmed.length() < 2) {
            throw new InvalidDataException(fieldName,
                    "El " + fieldName + " debe contener al menos 2 caracteres.");
        }
        if (!LETTERS_AND_SPACES_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidDataException(fieldName,
                    "El " + fieldName + " no puede contener números ni caracteres especiales.");
        }
    }

    /**
     * Validates that an email address is non-empty, contains the {@code @} symbol,
     * and has a valid user and domain structure without spaces.
     *
     * @param email email address to validate
     * @throws InvalidDataException if the email format is invalid
     */
    public static void validateEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidDataException("correo", "El correo electrónico es obligatorio.");
        }
        String trimmed = email.trim();
        if (!trimmed.contains("@")) {
            throw new InvalidDataException("correo",
                    "El correo electrónico debe contener el símbolo '@'.");
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidDataException("correo",
                    "El correo electrónico '" + trimmed + "' no tiene un formato válido (ejemplo: usuario@institucion.edu.co).");
        }
    }

    /**
     * Validates the descriptive institutional role of the user (e.g., Estudiante,
     * Docente, Inspector Ambiental, Coordinador).
     *
     * @param role descriptive institutional role
     * @throws InvalidDataException if the role is blank or contains numbers/invalid characters
     */
    public static void validateInstitutionalRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new InvalidDataException("rol",
                    "El rol institucional del usuario es obligatorio.");
        }
        String trimmed = role.trim();
        if (!LETTERS_AND_SPACES_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidDataException("rol",
                    "El rol institucional solo debe contener letras y espacios (sin números).");
        }
    }

    /**
     * Verifies that the given email address is not already registered by another user.
     *
     * @param email         email address to verify
     * @param existingUsers list of currently registered users
     * @throws BusinessRuleException if another user already has the same email address
     */
    public static void validateUniqueEmail(String email, List<User> existingUsers) {
        validateEmailFormat(email);
        if (existingUsers == null) {
            return;
        }
        String normalized = email.trim();
        for (User existing : existingUsers) {
            if (existing != null && existing.getEmail() != null
                    && existing.getEmail().trim().equalsIgnoreCase(normalized)) {
                throw new BusinessRuleException(
                        "El correo electrónico '" + normalized + "' ya se encuentra registrado en el sistema.");
            }
        }
    }

    /**
     * Verifies that a looked-up user instance is not null.
     *
     * @param user       user reference to check
     * @param identifier looked-up ID or email
     * @throws ResourceNotFoundException if {@code user} is {@code null}
     */
    public static void validateUserExists(User user, Object identifier) {
        if (user == null) {
            throw new ResourceNotFoundException("Usuario", identifier);
        }
    }
}