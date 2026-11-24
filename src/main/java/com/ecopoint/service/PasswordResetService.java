package com.ecopoint.service;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.PasswordResetToken;
import com.ecopoint.model.User;
import com.ecopoint.persistence.PasswordResetTokenRepository;
import com.ecopoint.persistence.UserRepository;
import com.ecopoint.security.LoginAttemptService;
import com.ecopoint.validation.UserValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Recovers the password with a single-use link sent by email (spec D18,
 * RP-01 to RP-08). Asking for a link never tells whether the email has an
 * account.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class PasswordResetService {

    /** Message for a link that was used, replaced, expired or never existed (RP-08). */
    public static final String INVALID_LINK = "El enlace no es válido o ya venció. Solicita uno nuevo.";
    /** Links a user can receive in one hour (RP-05). */
    public static final int MAX_LINKS_PER_HOUR = 3;

    private static final int CODE_BYTES = 32;

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetMailer mailer;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    /**
     * Creates the service.
     *
     * @param users           user repository
     * @param tokens          repository of recovery codes
     * @param mailer          sends the link by email
     * @param passwordEncoder BCrypt password encoder
     * @param loginAttempts   failed login counter, cleared after the change
     * @param clock           application clock
     */
    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, PasswordResetMailer mailer,
                                PasswordEncoder passwordEncoder, LoginAttemptService loginAttempts, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.mailer = mailer;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.clock = clock;
    }

    /**
     * Sends a link to the email if it belongs to an active account that has not
     * reached the limit. It returns the same way in every case (RP-01, RP-02, RP-05).
     *
     * @param email email typed by the user
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> user = email == null || email.isBlank()
                ? Optional.empty()
                : users.findByEmail(email.trim()).filter(User::isActive);
        if (user.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Long userId = user.get().getId();
        if (tokens.countCreatedSince(userId, now.minusHours(1)) >= MAX_LINKS_PER_HOUR) {
            return;
        }
        tokens.invalidateAll(userId, now);
        String code = newCode();
        tokens.save(PasswordResetToken.issue(userId, hash(code), now));
        mailer.sendResetLink(user.get(), code);
    }

    /**
     * Saves the new password with a code received by email. The code and every
     * other code of the user stop working (RP-03, RP-06, RP-08).
     *
     * @param code        code of the link
     * @param newPassword new password, with the same rules as when a user is created
     * @return the user whose password changed, so that its sessions can be closed (RP-07)
     * @throws InvalidDataException if the link does not work or the password is invalid
     */
    @Transactional
    public User confirmReset(String code, String newPassword) {
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken token = (code == null || code.isBlank() ? Optional.<PasswordResetToken>empty()
                : tokens.findByHash(hash(code.trim())))
                .filter(found -> found.isUsable(now))
                .orElseThrow(() -> new InvalidDataException(INVALID_LINK));
        User user = users.findById(token.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidDataException(INVALID_LINK));
        UserValidator.validatePassword(newPassword);
        if (!tokens.markUsed(token.getId(), now)) {
            throw new InvalidDataException(INVALID_LINK);
        }
        tokens.invalidateAll(user.getId(), now);
        users.updatePasswordHash(user.getId(), passwordEncoder.encode(newPassword));
        loginAttempts.registerSuccess(user.getEmail());
        return user;
    }

    /**
     * Computes the SHA-256 of a code, the only form in which it is stored (RP-04).
     *
     * @param code code of the link
     * @return hash in lower-case hexadecimal
     */
    public static String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new PersistenceException("proteger el código de recuperación", e);
        }
    }

    private String newCode() {
        byte[] bytes = new byte[CODE_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
