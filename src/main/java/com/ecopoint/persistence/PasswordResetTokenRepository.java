package com.ecopoint.persistence;

import com.ecopoint.model.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Codes to recover the password by email (spec D18).
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface PasswordResetTokenRepository {

    /**
     * Inserts a new code.
     *
     * @param token code to save, with id null
     * @return the same code, with its id assigned
     */
    PasswordResetToken save(PasswordResetToken token);

    /**
     * Finds a code by the SHA-256 of the code.
     *
     * @param codeHash SHA-256 in lower-case hexadecimal
     * @return the code, or empty if it does not exist
     */
    Optional<PasswordResetToken> findByHash(String codeHash);

    /**
     * Counts the codes created for a user since a moment (RP-05).
     *
     * @param userId user id
     * @param since  start of the period
     * @return number of codes
     */
    int countCreatedSince(Long userId, LocalDateTime since);

    /**
     * Marks a code as used, only if nobody used or replaced it before.
     *
     * @param tokenId code id
     * @param at      moment it is used
     * @return true if this call marked it; false if it was already used or replaced
     */
    boolean markUsed(Long tokenId, LocalDateTime at);

    /**
     * Marks every unused code of a user as replaced (RP-03).
     *
     * @param userId user id
     * @param at     moment they stop working
     */
    void invalidateAll(Long userId, LocalDateTime at);
}
