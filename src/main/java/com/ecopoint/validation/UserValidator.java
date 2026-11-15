package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Stateless validator for user data and the unique email rule (RN-080, RN-084),
 * applying GRASP Pure Fabrication and SOLID Single Responsibility Principle.
 *
 * @author craulgomez
 * @version 2.0
 */
public final class UserValidator {

    private static final Pattern LETTERS_AND_SPACES_PATTERN =
            Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+(\\s+[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+)*$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 80;
    /** Shortest password accepted when a teacher creates or changes it (spec 003, R-120). */
    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private UserValidator() {
    }

    /**
     * Validates all the data of a user before creating or updating it.
     *
     * @param user user to validate
     * @throws InvalidDataException if a field is missing or has an invalid format
     */
    public static void validate(User user) {
        if (user == null) {
            throw new InvalidDataException("usuario", "El usuario no puede ser nulo.");
        }
        validatePersonName(user.getFirstName(), "nombre");
        validatePersonName(user.getLastName(), "apellido");
        validateEmailFormat(user.getEmail());
        if (user.getRole() == null) {
            throw new InvalidDataException("rol", "El rol es obligatorio: Docente o Estudiante universitario.");
        }
        if (user.getInstitutionId() == null) {
            throw new InvalidDataException("institucion", "El usuario debe pertenecer a una institución.");
        }
    }

    /**
     * Validates that no other user has the same email, ignoring spaces and
     * upper or lower case (RN-084). When editing, the caller must exclude the
     * user's own current email from the set.
     *
     * @param user           user being created or edited
     * @param existingEmails emails of the other users
     * @throws InvalidDataException if the user is null
     * @throws BusinessRuleException if the email is already registered
     */
    public static void validateUniqueEmail(User user, Set<String> existingEmails) {
        if (user == null) {
            throw new InvalidDataException("usuario", "El usuario no puede ser nulo.");
        }
        if (existingEmails == null) {
            return;
        }
        String normalized = User.normalizeEmail(user.getEmail());
        boolean taken = existingEmails.stream()
                .map(User::normalizeEmail)
                .anyMatch(normalized::equals);
        if (taken) {
            throw new BusinessRuleException("El correo " + user.getEmail().trim()
                    + " ya está registrado por otro usuario.");
        }
    }

    /**
     * Validates a new password before it is hashed (spec 003, R-120). BCrypt
     * only uses the first 72 bytes, so longer passwords are rejected.
     *
     * @param password password typed by the teacher
     * @throws InvalidDataException if it is missing, too short or too long
     */
    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidDataException("contrasena", "La contraseña es obligatoria.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new InvalidDataException("contrasena", "La contraseña debe tener entre " + MIN_PASSWORD_LENGTH
                    + " y " + MAX_PASSWORD_LENGTH + " caracteres.");
        }
    }

    private static void validatePersonName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException(field, "El " + field + " es obligatorio.");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidDataException(field,
                    "El " + field + " debe tener entre " + MIN_NAME_LENGTH + " y " + MAX_NAME_LENGTH + " caracteres.");
        }
        if (!LETTERS_AND_SPACES_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidDataException(field,
                    "El " + field + " solo puede contener letras y espacios.");
        }
    }

    /**
     * Validates that a user may be deactivated: only university students are
     * deactivated; a teacher never is, not even by himself, so the institution
     * always keeps someone who administers it (RN-087).
     *
     * @param user user to deactivate
     * @throws BusinessRuleException if the user is a teacher
     */
    public static void validateDeactivation(User user) {
        if (user == null) {
            throw new InvalidDataException("usuario", "El usuario no puede ser nulo.");
        }
        if (user.getRole() == UserRole.DOCENTE) {
            throw new BusinessRuleException("Un docente no se puede desactivar: solo se desactivan estudiantes.");
        }
    }

    private static void validateEmailFormat(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidDataException("correo", "El correo electrónico es obligatorio.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new InvalidDataException("correo",
                    "El correo electrónico no tiene un formato válido (ej. nombre@unicesar.edu.co).");
        }
    }
}
