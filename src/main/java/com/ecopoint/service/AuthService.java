package com.ecopoint.service;

import com.ecopoint.exceptions.UnauthorizedException;
import com.ecopoint.model.Institution;
import com.ecopoint.model.User;
import com.ecopoint.persistence.InstitutionRepository;
import com.ecopoint.persistence.UserRepository;
import com.ecopoint.security.LoginAttemptService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Login and current user of the session (spec 003, R-120, R-125, R-126).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class AuthService {

    /** Message for any failed login; it never reveals whether the email exists (CA-202). */
    public static final String INVALID_CREDENTIALS = "Correo o contraseña incorrectos.";
    /** Message while the account is locked (R-126). */
    public static final String ACCOUNT_LOCKED = "Cuenta bloqueada temporalmente. Intente en 15 minutos.";
    /** Message when the user of the session no longer exists or was deactivated. */
    public static final String SESSION_INVALID = "Su sesión ya no es válida. Inicie sesión de nuevo.";

    private final UserRepository users;
    private final InstitutionRepository institutions;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    /**
     * Hash compared when the email does not exist, so a failed login takes the
     * same time whether the email exists or not.
     */
    private final String dummyHash;

    /**
     * Creates the service.
     *
     * @param users           user repository
     * @param institutions    institution repository
     * @param passwordEncoder BCrypt password encoder
     * @param loginAttempts   failed login counter
     */
    public AuthService(UserRepository users, InstitutionRepository institutions, PasswordEncoder passwordEncoder,
                       LoginAttemptService loginAttempts) {
        this.users = users;
        this.institutions = institutions;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.dummyHash = passwordEncoder.encode("ecopoint-usuario-inexistente");
    }

    /**
     * Checks the email and the password.
     *
     * @param email    email typed by the user
     * @param password password typed by the user
     * @return the user with its institution
     * @throws UnauthorizedException if the data is wrong, the account is inactive or locked
     */
    public AuthenticatedUser login(String email, String password) {
        if (loginAttempts.isLocked(email)) {
            throw new UnauthorizedException(ACCOUNT_LOCKED);
        }
        Optional<User> user = email == null ? Optional.empty() : users.findByEmail(email);
        Optional<String> hash = user.flatMap(found -> users.findPasswordHashByEmail(email));
        boolean passwordMatches = password != null && passwordEncoder.matches(password, hash.orElse(dummyHash));
        if (user.isEmpty() || hash.isEmpty() || !user.get().isActive() || !passwordMatches) {
            loginAttempts.registerFailure(email);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        loginAttempts.registerSuccess(email);
        return withInstitution(user.get());
    }

    /**
     * Reloads the user of the session, so a deactivation takes effect at once.
     *
     * @param userId id of the user kept in the session
     * @return the user with its institution
     * @throws UnauthorizedException if the user no longer exists or is inactive
     */
    public AuthenticatedUser currentUser(Long userId) {
        User user = users.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException(SESSION_INVALID));
        return withInstitution(user);
    }

    private AuthenticatedUser withInstitution(User user) {
        Institution institution = institutions.findById(user.getInstitutionId()).orElse(null);
        return new AuthenticatedUser(user, institution);
    }
}
