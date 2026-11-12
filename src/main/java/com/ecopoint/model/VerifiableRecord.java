package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;

/**
 * Base class of every field record that is typed in the app and then checked
 * against the paper form: shift population, waste bag, quartering and density
 * measurement (RN-060). Thanks to this common base the verification inbox can
 * treat all of them the same way (polymorphism).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public abstract class VerifiableRecord {

    private Long id;
    private final VerificationInfo verification;

    /**
     * Creates a record with its verification data.
     *
     * @param id           identifier, or null if not saved yet
     * @param verification who typed the record and when
     * @throws InvalidDataException if the verification data is missing
     */
    protected VerifiableRecord(Long id, VerificationInfo verification) {
        if (verification == null) {
            throw new InvalidDataException("verificacion",
                    "Todo registro de campo necesita los datos de quién lo digitó y cuándo.");
        }
        this.id = id;
        this.verification = verification;
    }

    /**
     * Returns the kind of record, in Spanish, to show it in the verification
     * inbox and in the correction history (e.g. "Bolsa", "Cuarteo").
     *
     * @return record type name in Spanish
     */
    public abstract String getRecordType();

    /**
     * Indicates whether the record still has to be checked against the paper form.
     *
     * @return true when pending
     */
    public boolean isPendingVerification() {
        return verification.isPending();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VerificationInfo getVerification() {
        return verification;
    }
}
