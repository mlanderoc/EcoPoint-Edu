package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Double-registration data of a field record: who typed it and when, and who
 * checked it against the paper form and when (RN-060 to RN-063). A new record
 * is always pending. The rule that the verifier must be another person
 * (RN-062) is enforced by {@code VerificationValidator}.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class VerificationInfo {

    private VerificationStatus status;
    private final Long enteredBy;
    private final LocalDateTime enteredAt;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;

    /**
     * Creates the verification data of a newly typed record, in PENDIENTE status.
     *
     * @param enteredBy identifier of the user who typed the record
     * @param enteredAt date and time the record was typed
     * @throws InvalidDataException if the user or the date is missing (RN-061)
     */
    public VerificationInfo(Long enteredBy, LocalDateTime enteredAt) {
        if (enteredBy == null) {
            throw new InvalidDataException("digitadoPor", "Debe registrarse quién digitó el dato.");
        }
        if (enteredAt == null) {
            throw new InvalidDataException("fechaDigitacion", "Debe registrarse cuándo se digitó el dato.");
        }
        this.status = VerificationStatus.PENDIENTE;
        this.enteredBy = enteredBy;
        this.enteredAt = enteredAt;
    }

    /**
     * Rebuilds the verification data of a record read from the database
     * (spec 002, plan R1). Only the persistence layer must use it.
     *
     * @param status     stored verification status
     * @param enteredBy  user who typed the record
     * @param enteredAt  when it was typed
     * @param verifiedBy user who verified or corrected it, or null
     * @param verifiedAt when it was verified or corrected, or null
     * @return the rebuilt verification data
     */
    public static VerificationInfo restore(VerificationStatus status, Long enteredBy, LocalDateTime enteredAt,
                                           Long verifiedBy, LocalDateTime verifiedAt) {
        VerificationInfo info = new VerificationInfo(enteredBy, enteredAt);
        info.status = status;
        info.verifiedBy = verifiedBy;
        info.verifiedAt = verifiedAt;
        return info;
    }

    /**
     * Marks the record as matching the paper form (RN-062).
     *
     * @param userId identifier of the verifier
     * @param at     date and time of the verification
     * @throws BusinessRuleException if the record is not pending
     * @throws InvalidDataException  if the verifier or the date is missing
     */
    public void markVerified(Long userId, LocalDateTime at) {
        if (!status.isPending()) {
            throw new BusinessRuleException("El registro ya fue " + status.getDisplayName().toLowerCase()
                    + "; no se puede verificar de nuevo.");
        }
        requireActor(userId, at);
        this.status = VerificationStatus.VERIFICADO;
        this.verifiedBy = userId;
        this.verifiedAt = at;
    }

    /**
     * Marks the record as corrected after checking it against the paper form
     * (RN-063). A pending or an already verified record may be corrected; the
     * old and new values are kept in {@link Correction} records.
     *
     * @param userId identifier of the user who corrected it
     * @param at     date and time of the correction
     * @throws InvalidDataException if the user or the date is missing
     */
    public void markCorrected(Long userId, LocalDateTime at) {
        requireActor(userId, at);
        this.status = VerificationStatus.CORREGIDO;
        this.verifiedBy = userId;
        this.verifiedAt = at;
    }

    /**
     * Indicates whether the record was typed by the given user.
     *
     * @param userId user identifier
     * @return true if that user typed the record
     */
    public boolean wasEnteredBy(Long userId) {
        return Objects.equals(enteredBy, userId);
    }

    /**
     * Indicates whether the record still has to be checked against the paper form.
     *
     * @return true when pending
     */
    public boolean isPending() {
        return status.isPending();
    }

    private static void requireActor(Long userId, LocalDateTime at) {
        if (userId == null) {
            throw new InvalidDataException("verificadoPor", "Debe registrarse quién verificó el dato.");
        }
        if (at == null) {
            throw new InvalidDataException("fechaVerificacion", "Debe registrarse cuándo se verificó el dato.");
        }
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public Long getEnteredBy() {
        return enteredBy;
    }

    public LocalDateTime getEnteredAt() {
        return enteredAt;
    }

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
}
