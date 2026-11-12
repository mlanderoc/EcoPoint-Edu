package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.model.VerifiableRecord;

/**
 * Stateless validator for the double registration (RN-060 to RN-063): who may
 * check a record against the paper form and who may correct it. Applies GRASP
 * Pure Fabrication and SOLID SRP.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class VerificationValidator {

    private VerificationValidator() {
    }

    /**
     * Validates that the study still accepts verifications and corrections:
     * while executing or in data cleaning (RN-014).
     *
     * @param study study the record belongs to
     * @throws BusinessRuleException if the study is planned or closed
     */
    public static void validateStudyAllowsVerification(CharacterizationStudy study) {
        if (study == null) {
            throw new InvalidDataException("estudio", "El estudio no puede ser nulo.");
        }
        if (!study.getStatus().allowsVerification()) {
            throw new BusinessRuleException("El estudio está " + study.getStatus().getDisplayName().toLowerCase()
                    + ": no admite verificaciones ni correcciones.");
        }
    }

    /**
     * Validates that the user may mark the record as matching the paper form:
     * the user has the permission, the record is pending and it was typed by
     * another person (RN-062).
     *
     * @param record record to verify
     * @param user   user verifying it
     * @throws com.ecopoint.exceptions.AccessDeniedException if the user lacks the permission
     * @throws BusinessRuleException if the record is not pending or the user typed it
     */
    public static void validateVerifier(VerifiableRecord record, User user) {
        requireRecord(record);
        AccessPolicy.require(user, Permission.VERIFY_RECORDS);
        if (!record.isPendingVerification()) {
            throw new BusinessRuleException("El registro ya fue "
                    + record.getVerification().getStatus().getDisplayName().toLowerCase() + ".");
        }
        requireAnotherPerson(record, user, "verificar");
    }

    /**
     * Validates that the user may correct the record after checking it
     * against the paper form: the user has the permission, did not type the
     * record and wrote a reason (RN-062, RN-063).
     *
     * @param record record to correct
     * @param user   user correcting it
     * @param reason reason for the correction
     * @throws com.ecopoint.exceptions.AccessDeniedException if the user lacks the permission
     * @throws BusinessRuleException if the user typed the record
     * @throws InvalidDataException  if the reason is missing
     */
    public static void validateCorrection(VerifiableRecord record, User user, String reason) {
        requireRecord(record);
        AccessPolicy.require(user, Permission.VERIFY_RECORDS);
        requireAnotherPerson(record, user, "corregir");
        if (reason == null || reason.isBlank()) {
            throw new InvalidDataException("motivo", "El motivo de la corrección es obligatorio.");
        }
    }

    private static void requireRecord(VerifiableRecord record) {
        if (record == null) {
            throw new InvalidDataException("registro", "El registro a verificar no puede ser nulo.");
        }
    }

    private static void requireAnotherPerson(VerifiableRecord record, User user, String action) {
        if (record.getVerification().wasEnteredBy(user.getId())) {
            throw new BusinessRuleException("No puede " + action + " un registro que usted mismo digitó: "
                    + "debe hacerlo otra persona (doble registro).");
        }
    }
}
