package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.GeoPoint;
import com.ecopoint.model.GeoPolygon;
import com.ecopoint.model.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GenerationSourceValidator} (RN-001 to RN-005, CA-001 to CA-003).
 *
 * @author craulgomez
 * @version 1.0
 */
class GenerationSourceValidatorTest {

    private static final Long INSTITUTION = 1L;

    private static GenerationSource source(String code) {
        return new GenerationSource(null, INSTITUTION, code, "Bloque de aulas 1", SourceType.AULAS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"au", "A", "AULAS12", "AU-1", "AU 1"})
    void ca001_invalidCodeFormatIsRejected(String code) {
        assertThrows(InvalidDataException.class, () -> GenerationSourceValidator.validate(source(code)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AU", "AU1", "PAE", "LAB", "AULAS1"})
    void ca001_validCodesAreAccepted(String code) {
        assertDoesNotThrow(() -> GenerationSourceValidator.validate(source(code)));
    }

    @Test
    void ca002_duplicatedCodeInSameInstitutionIsRejected() {
        assertThrows(BusinessRuleException.class,
                () -> GenerationSourceValidator.validateUniqueCode(source("AU"), Set.of("AU", "OF")));
    }

    @Test
    void ca002_sameCodeInAnotherInstitutionIsAccepted() {
        Set<String> codesOfOtherInstitutionAreNotPassed = Set.of("OF");

        assertDoesNotThrow(
                () -> GenerationSourceValidator.validateUniqueCode(source("AU"), codesOfOtherInstitutionAreNotPassed));
    }

    @Test
    void ca003_sourceCannotHaveAnOpenPolygon() {
        GeoPoint a = GeoPoint.of("10.4630", "-73.2530");
        GeoPoint b = GeoPoint.of("10.4630", "-73.2520");
        GeoPoint c = GeoPoint.of("10.4640", "-73.2520");

        assertThrows(InvalidDataException.class, () -> source("AU").setGeometry(new GeoPolygon(List.of(a, b, c))));
    }

    @Test
    void rn004_sourceWithClosedPolygonIsValid() {
        GeoPoint a = GeoPoint.of("10.4630", "-73.2530");
        GeoPoint b = GeoPoint.of("10.4630", "-73.2520");
        GeoPoint c = GeoPoint.of("10.4640", "-73.2520");
        GenerationSource source = source("AU");

        source.setGeometry(new GeoPolygon(List.of(a, b, c, a)));

        assertTrue(source.hasGeometry());
        assertDoesNotThrow(() -> GenerationSourceValidator.validate(source));
    }

    @Test
    void rn001_sourceWithoutInstitutionIsRejected() {
        GenerationSource source = new GenerationSource(null, null, "AU", "Aulas", SourceType.AULAS);

        assertThrows(InvalidDataException.class, () -> GenerationSourceValidator.validate(source));
    }

    @Test
    void rn003_sourceWithoutTypeOrNameIsRejected() {
        assertThrows(InvalidDataException.class, () -> GenerationSourceValidator.validate(
                new GenerationSource(null, INSTITUTION, "AU", "Aulas", null)));
        assertThrows(InvalidDataException.class, () -> GenerationSourceValidator.validate(
                new GenerationSource(null, INSTITUTION, "AU", " ", SourceType.AULAS)));
    }

    @Test
    void rn005_sourceWithBagsCannotBeDeleted() {
        assertThrows(BusinessRuleException.class,
                () -> GenerationSourceValidator.validateDeletion(source("AU"), true));
        assertDoesNotThrow(() -> GenerationSourceValidator.validateDeletion(source("AU"), false));
    }

    @Test
    void rn005_inactiveSourceIsNotAvailableForNewRecords() {
        GenerationSource source = source("AU");

        source.deactivate();

        assertFalse(source.isAvailableForRegistration());
        assertThrows(BusinessRuleException.class,
                () -> GenerationSourceValidator.validateAvailableForRegistration(source));
    }
}
