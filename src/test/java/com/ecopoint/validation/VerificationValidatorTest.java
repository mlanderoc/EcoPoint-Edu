package com.ecopoint.validation;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Correction;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.VerificationStatus;
import com.ecopoint.model.WasteBag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link VerificationValidator} (RN-060 to RN-063, CA-060 to CA-062).
 *
 * @author craulgomez
 * @version 1.0
 */
class VerificationValidatorTest {

    private static final LocalDateTime TYPED = LocalDateTime.of(2026, 3, 4, 12, 10);
    private static final LocalDateTime CHECKED = LocalDateTime.of(2026, 3, 4, 16, 0);

    private static final User USER_A =
            new User(1L, "Ana", "Pérez", "ana@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    private static final User USER_B =
            new User(2L, "Luis", "Gómez", "luis@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);

    private static WasteBag bagTypedByA() {
        return new WasteBag(7L, 1L, "AU", new BigDecimal("12.40"), new BigDecimal("0.40"),
                new BigDecimal("0.080"), new VerificationInfo(USER_A.getId(), TYPED));
    }

    @Test
    void ca060_typistCannotVerifyButAnotherPersonCan() {
        WasteBag bag = bagTypedByA();

        assertThrows(BusinessRuleException.class, () -> VerificationValidator.validateVerifier(bag, USER_A));
        assertDoesNotThrow(() -> VerificationValidator.validateVerifier(bag, USER_B));

        bag.getVerification().markVerified(USER_B.getId(), CHECKED);
        assertEquals(VerificationStatus.VERIFICADO, bag.getVerification().getStatus());
        assertEquals(USER_B.getId(), bag.getVerification().getVerifiedBy());
        assertEquals(CHECKED, bag.getVerification().getVerifiedAt());
    }

    @Test
    void ca061_correctionKeepsTheOriginalValueAndTheReason() {
        WasteBag bag = bagTypedByA();
        assertDoesNotThrow(() -> VerificationValidator.validateCorrection(bag, USER_B, "error de digitación"));

        Correction correction = Correction.of(bag, "pesoBruto", bag.getGrossWeight(), new BigDecimal("12.04"),
                "error de digitación", USER_B.getId(), CHECKED);
        bag.setGrossWeight(new BigDecimal("12.04"));
        bag.getVerification().markCorrected(USER_B.getId(), CHECKED);

        assertEquals(VerificationStatus.CORREGIDO, bag.getVerification().getStatus());
        assertEquals("12.40", correction.oldValue());
        assertEquals("12.04", correction.newValue());
        assertEquals("error de digitación", correction.reason());
        assertEquals(USER_B.getId(), correction.userId());
        assertEquals(CHECKED, correction.at());
    }

    @Test
    void ca062_universityStudentVerifiesARecordTypedBySomeoneElse() {
        assertDoesNotThrow(() -> VerificationValidator.validateVerifier(bagTypedByA(), USER_B));
    }

    @Test
    void rn063_correctionNeedsAReasonAndAnotherPerson() {
        WasteBag bag = bagTypedByA();

        assertThrows(InvalidDataException.class, () -> VerificationValidator.validateCorrection(bag, USER_B, " "));
        assertThrows(BusinessRuleException.class,
                () -> VerificationValidator.validateCorrection(bag, USER_A, "error de digitación"));
    }

    @Test
    void rn062_alreadyVerifiedRecordIsNotVerifiedAgain() {
        WasteBag bag = bagTypedByA();
        bag.getVerification().markVerified(USER_B.getId(), CHECKED);
        User teacher = new User(3L, "Oriana", "Jurado", "oriana@unicesar.edu.co", UserRole.DOCENTE, 1L);

        assertThrows(BusinessRuleException.class, () -> VerificationValidator.validateVerifier(bag, teacher));
    }

    @Test
    void rn081_inactiveUserCannotVerify() {
        User inactive = new User(4L, "Eva", "Ríos", "eva@unicesar.edu.co", UserRole.DOCENTE, 1L);
        inactive.deactivate();

        assertThrows(AccessDeniedException.class, () -> VerificationValidator.validateVerifier(bagTypedByA(), inactive));
    }

    @Test
    void rn014_verificationOnlyWhileExecutingOrCleaning() {
        CharacterizationStudy study = new CharacterizationStudy(1L, 1L);
        assertThrows(BusinessRuleException.class, () -> VerificationValidator.validateStudyAllowsVerification(study));

        study.changeStatus(StudyStatus.EN_EJECUCION);
        assertDoesNotThrow(() -> VerificationValidator.validateStudyAllowsVerification(study));
        study.changeStatus(StudyStatus.EN_DEPURACION);
        assertDoesNotThrow(() -> VerificationValidator.validateStudyAllowsVerification(study));
        study.changeStatus(StudyStatus.CERRADO);
        assertThrows(BusinessRuleException.class, () -> VerificationValidator.validateStudyAllowsVerification(study));
    }
}
