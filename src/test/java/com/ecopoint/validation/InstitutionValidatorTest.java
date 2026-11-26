package com.ecopoint.validation;

import java.math.BigDecimal;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.Institution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link InstitutionValidator} and the defaults of {@link Institution}.
 *
 * @author craulgomez
 * @version 1.0
 */
class InstitutionValidatorTest {

    private static Institution pilot() {
        return new Institution(null, "I.E. Prudencia Daza", "Calle 16 # 19 - 32, Valledupar, Cesar");
    }

    @Test
    void validInstitutionIsAccepted() {
        assertDoesNotThrow(() -> InstitutionValidator.validate(pilot()));
    }

    @Test
    void newInstitutionUsesDefaultSettingsAndZoom() {
        Institution institution = pilot();

        assertEquals(CharacterizationSettings.defaults(), institution.getSettings());
        assertEquals(Institution.DEFAULT_MAP_ZOOM, institution.getMapZoom());
    }

    @Test
    void nullSettingsRestoreTheDefaults() {
        Institution institution = pilot();

        institution.setSettings(null);

        assertEquals(CharacterizationSettings.defaults(), institution.getSettings());
    }

    @Test
    void missingNameOrAddressIsRejected() {
        assertThrows(InvalidDataException.class,
                () -> InstitutionValidator.validate(new Institution(null, " ", "Valledupar")));
        assertThrows(InvalidDataException.class,
                () -> InstitutionValidator.validate(new Institution(null, "I.E. Prudencia Daza", null)));
    }

    @Test
    void mapZoomOutOfRangeIsRejected() {
        Institution institution = pilot();
        institution.setMapZoom(25);

        assertThrows(InvalidDataException.class, () -> InstitutionValidator.validate(institution));
    }

    @Test
    void rev2_addressesOfValleduparAreAccepted() {
        assertDoesNotThrow(() -> InstitutionValidator.validateAddress("Calle 16A # 12B - 45, Novalito, Valledupar, Cesar"));
        assertDoesNotThrow(() -> InstitutionValidator.validateAddress("Carrera 9 Bis # 15 - 20, Valledupar, Cesar"));
        assertDoesNotThrow(() -> InstitutionValidator.validateAddress(
                "Diagonal 21 # 5A - 199, Los Mayales, Sede principal, Valledupar, Cesar"));
        assertDoesNotThrow(() -> InstitutionValidator.validateAddress("Kilómetro 5 vía a La Paz, Valledupar, Cesar"));
    }

    @Test
    void rev2_addressesWithoutTheNomenclatureOrWithIllogicalNumbersAreRejected() {
        assertThrows(InvalidDataException.class, () -> InstitutionValidator.validateAddress("Valledupar, Cesar"));
        assertThrows(InvalidDataException.class, () -> InstitutionValidator.validateAddress("Calle 16 # 12 - 45"));
        assertThrows(InvalidDataException.class,
                () -> InstitutionValidator.validateAddress("Calle 16 # 12 - 45, Bogotá, Cundinamarca"));
        assertThrows(InvalidDataException.class,
                () -> InstitutionValidator.validateAddress("Calle 0 # 12 - 45, Valledupar, Cesar"));
        assertThrows(InvalidDataException.class,
                () -> InstitutionValidator.validateAddress("Carrera 151 # 12 - 45, Valledupar, Cesar"));
        assertThrows(InvalidDataException.class,
                () -> InstitutionValidator.validateAddress("Calle 16 # 12 - 200, Valledupar, Cesar"));
        assertThrows(InvalidDataException.class,
                () -> InstitutionValidator.validateAddress("Kilómetro 31 vía a La Paz, Valledupar, Cesar"));
    }

    @Test
    void rev1_settingsOutsideTheirLogicalRangeAreRejected() {
        CharacterizationSettings tooTolerant = new CharacterizationSettings(new BigDecimal("45"), new BigDecimal("55"),
                new BigDecimal("15"), new BigDecimal("50"), new BigDecimal("20"), new BigDecimal("1000"));
        CharacterizationSettings hugeBag = new CharacterizationSettings(new BigDecimal("45"), new BigDecimal("55"),
                new BigDecimal("2"), new BigDecimal("150"), new BigDecimal("20"), new BigDecimal("1000"));

        assertThrows(InvalidDataException.class, () -> InstitutionValidator.validateSettings(tooTolerant));
        assertThrows(InvalidDataException.class, () -> InstitutionValidator.validateSettings(hugeBag));
        assertDoesNotThrow(() -> InstitutionValidator.validateSettings(CharacterizationSettings.defaults()));
    }
}
