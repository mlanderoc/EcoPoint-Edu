package com.ecopoint.validation;

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
        return new Institution(null, "I.E. Prudencia Daza", "Valledupar, Cesar");
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
}
