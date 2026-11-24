package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Single-use code sent by email to create a new password (spec D18, RP-03 and
 * RP-04). It keeps only the SHA-256 hash of the code, never the code itself.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class PasswordResetToken {

    /** Time a code can be used after it is created (RP-03). */
    public static final Duration VALIDITY = Duration.ofMinutes(30);

    private Long id;
    private final Long userId;
    private final String codeHash;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private LocalDateTime usedAt;

    /**
     * Rebuilds a stored code.
     *
     * @param id        identifier, or null if not saved yet
     * @param userId    user the code belongs to
     * @param codeHash  SHA-256 of the code, in lower-case hexadecimal
     * @param createdAt moment it was created
     * @param expiresAt moment it stops working
     * @param usedAt    moment it was used or replaced, or null
     */
    public PasswordResetToken(Long id, Long userId, String codeHash, LocalDateTime createdAt,
                              LocalDateTime expiresAt, LocalDateTime usedAt) {
        this.id = id;
        this.userId = userId;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    /**
     * Creates a new code that works for {@link #VALIDITY}.
     *
     * @param userId   user the code belongs to
     * @param codeHash SHA-256 of the code
     * @param at       moment it is created
     * @return the new, unsaved code
     */
    public static PasswordResetToken issue(Long userId, String codeHash, LocalDateTime at) {
        return new PasswordResetToken(null, userId, codeHash, at, at.plus(VALIDITY), null);
    }

    /**
     * Tells whether the code can still be used.
     *
     * @param at moment to check
     * @return true if it was not used or replaced and has not expired
     */
    public boolean isUsable(LocalDateTime at) {
        return usedAt == null && at.isBefore(expiresAt);
    }

    /**
     * Marks the code as used.
     *
     * @param at moment it is used
     * @throws BusinessRuleException if it was already used or replaced, or it expired
     */
    public void use(LocalDateTime at) {
        if (!isUsable(at)) {
            throw new BusinessRuleException("El enlace no es válido o ya venció.");
        }
        usedAt = at;
    }

    /**
     * @return identifier, or null if not saved yet
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id identifier assigned by the database
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return user the code belongs to
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * @return SHA-256 of the code
     */
    public String getCodeHash() {
        return codeHash;
    }

    /**
     * @return moment it was created
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * @return moment it stops working
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * @return moment it was used or replaced, or null
     */
    public LocalDateTime getUsedAt() {
        return usedAt;
    }
}
