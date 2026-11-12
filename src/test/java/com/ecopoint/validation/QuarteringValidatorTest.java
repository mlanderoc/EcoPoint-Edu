package com.ecopoint.validation;

import com.ecopoint.exceptions.AccessDeniedException;
import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link QuarteringValidator} (RN-050 to RN-056, CA-050, CA-051,
 * CA-055, CA-056).
 *
 * @author craulgomez
 * @version 2.0
 */
class QuarteringValidatorTest {

    private static final CharacterizationSettings SETTINGS = CharacterizationSettings.defaults();
    private static final LocalDateTime AT = LocalDateTime.of(2026, 3, 4, 13, 0);
    private static final WasteCategory PAPER = new WasteCategory(1L, "Papel", WasteGroup.BLANCO, null);
    private static final WasteCategory FOOD = new WasteCategory(2L, "Restos de comida", WasteGroup.VERDE, null);
    private static final WasteCategory BATTERIES = new WasteCategory(3L, "Pilas", WasteGroup.SIN_COLOR, null);
    private static final Map<Long, WasteCategory> CATALOG = Map.of(1L, PAPER, 2L, FOOD, 3L, BATTERIES);

    private static Quartering quartering(String shiftNet, String sample) {
        Quartering quartering = new Quartering(null, 1L, new BigDecimal(shiftNet), new BigDecimal(sample),
                new VerificationInfo(10L, AT));
        quartering.addResult(QuarteringResult.of(PAPER, new BigDecimal("20.00")));
        quartering.addResult(QuarteringResult.of(FOOD, new BigDecimal("29.50")));
        return quartering;
    }

    private static User student() {
        return new User(2L, "Luis", "Gómez", "luis@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    }

    @Test
    void rn051_sampleInsideTheRangeIsAccepted() {
        assertDoesNotThrow(() -> QuarteringValidator.validate(quartering("120.00", "50.00"), SETTINGS));
    }

    @Test
    void ca050_sampleOutsideTheRangeIsRejected() {
        assertThrows(BusinessRuleException.class,
                () -> QuarteringValidator.validate(quartering("120.00", "40.00"), SETTINGS));
        assertThrows(BusinessRuleException.class,
                () -> QuarteringValidator.validate(quartering("120.00", "60.00"), SETTINGS));
    }

    @Test
    void ca051_smallShiftIsAcceptedAsFullSample() {
        assertDoesNotThrow(() -> QuarteringValidator.validate(quartering("30.00", "30.00"), SETTINGS));
    }

    @Test
    void rn051_smallShiftMustBeClassifiedCompletely() {
        assertThrows(BusinessRuleException.class,
                () -> QuarteringValidator.validate(quartering("30.00", "25.00"), SETTINGS));
    }

    @Test
    void rn051_sampleCannotExceedTheShiftNetWeight() {
        assertThrows(BusinessRuleException.class,
                () -> QuarteringValidator.validate(quartering("48.00", "50.00"), SETTINGS));
    }

    @Test
    void rn055_negativeOrMissingSubcategoryWeightIsRejected() {
        Quartering quartering = new Quartering(null, 1L, new BigDecimal("120"), new BigDecimal("50"),
                new VerificationInfo(10L, AT));
        quartering.addResult(QuarteringResult.of(PAPER, new BigDecimal("-1")));

        assertThrows(InvalidDataException.class, () -> QuarteringValidator.validate(quartering, SETTINGS));
    }

    @Test
    void ca055_shiftWithQuarteringCannotReceiveAnother() {
        SamplingShift shift = new SamplingShift(1L, 1L, 3, ShiftType.MANANA);
        shift.registerQuartering(quartering("120.00", "50.00"));

        assertThrows(BusinessRuleException.class, () -> QuarteringValidator.validateNoExistingQuartering(shift));
    }

    @Test
    void rn055_repeatedOrUnknownSubcategoryIsRejected() {
        Quartering repeated = quartering("120.00", "50.00");
        repeated.addResult(QuarteringResult.of(PAPER, new BigDecimal("0.50")));
        assertThrows(BusinessRuleException.class, () -> QuarteringValidator.validateCategories(repeated, CATALOG));

        Quartering unknown = quartering("120.00", "50.00");
        unknown.addResult(new QuarteringResult(99L, "Desconocida", WasteGroup.NEGRO, BigDecimal.ONE));
        assertThrows(BusinessRuleException.class, () -> QuarteringValidator.validateCategories(unknown, CATALOG));

        assertDoesNotThrow(() -> QuarteringValidator.validateCategories(quartering("120.00", "50.00"), CATALOG));
    }

    @Test
    void ca056_universityStudentMayRegisterBatteries() {
        Quartering withBatteries = quartering("120.00", "50.00");
        withBatteries.addResult(QuarteringResult.of(BATTERIES, new BigDecimal("0.30")));

        assertDoesNotThrow(() -> QuarteringValidator.validatePermissions(withBatteries, student()));
    }

    @Test
    void rn056_inactiveUserCannotRegisterQuartering() {
        User inactive = student();
        inactive.deactivate();

        assertThrows(AccessDeniedException.class,
                () -> QuarteringValidator.validatePermissions(quartering("120.00", "50.00"), inactive));
    }

    @Test
    void rn052_onlyNonConformingQuarteringCanBeAnnulled() {
        Quartering conforming = quartering("120.00", "50.00");
        conforming.evaluateQuality(SETTINGS.qualityTolerancePercent());
        assertThrows(BusinessRuleException.class, () -> QuarteringValidator.validateAnnul(conforming, student()));

        Quartering nonConforming = new Quartering(null, 1L, new BigDecimal("120"), new BigDecimal("50"),
                new VerificationInfo(10L, AT));
        nonConforming.addResult(QuarteringResult.of(PAPER, new BigDecimal("45")));
        nonConforming.evaluateQuality(SETTINGS.qualityTolerancePercent());
        assertDoesNotThrow(() -> QuarteringValidator.validateAnnul(nonConforming, student()));
    }
}
