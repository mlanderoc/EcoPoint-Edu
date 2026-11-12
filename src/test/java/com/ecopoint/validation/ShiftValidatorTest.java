package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.VerificationInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link ShiftValidator} (RN-020 to RN-025, CA-020 to CA-023).
 *
 * @author craulgomez
 * @version 1.0
 */
class ShiftValidatorTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 3, 4, 6, 30);

    private static CharacterizationStudy executingStudy() {
        CharacterizationStudy study = new CharacterizationStudy(1L, 1L);
        study.changeStatus(StudyStatus.EN_EJECUCION);
        return study;
    }

    private static ShiftPopulation population(int students, int teachers, int staff) {
        return new ShiftPopulation(null, students, teachers, staff, new VerificationInfo(10L, AT));
    }

    private static SamplingShift openShift() {
        return new SamplingShift(5L, 1L, 3, ShiftType.MANANA);
    }

    @Test
    void ca020_secondMorningShiftOnTheSameDayIsRejected() {
        List<SamplingShift> existing = List.of(openShift());

        assertThrows(BusinessRuleException.class,
                () -> ShiftValidator.validateNewShift(executingStudy(), 3, ShiftType.MANANA, existing));
        assertDoesNotThrow(() -> ShiftValidator.validateNewShift(executingStudy(), 3, ShiftType.TARDE, existing));
    }

    @Test
    void rn014_shiftsAreOnlyOpenedWhileTheStudyIsExecuting() {
        assertThrows(BusinessRuleException.class,
                () -> ShiftValidator.validateNewShift(new CharacterizationStudy(1L, 1L), 1, ShiftType.MANANA, List.of()));
    }

    @Test
    void ca021_populationOfZeroPeopleIsRejected() {
        assertThrows(InvalidDataException.class, () -> ShiftValidator.validatePopulation(population(0, 0, 0)));
    }

    @Test
    void rn022_negativeCountsAreRejected() {
        InvalidDataException exception = assertThrows(InvalidDataException.class,
                () -> ShiftValidator.validatePopulation(population(-1, 5, 2)));
        assertEquals("estudiantes", exception.getFieldName());
    }

    @Test
    void rn022_validPopulationIsAccepted() {
        assertDoesNotThrow(() -> ShiftValidator.validatePopulation(population(420, 25, 18)));
    }

    @Test
    void ca022_bagWithoutPopulationIsRejectedWithClearMessage() {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> ShiftValidator.validateCanRegisterBag(openShift(), executingStudy()));

        assertEquals("Registre primero la población del turno.", exception.getMessage());
    }

    @Test
    void rn023_bagIsAcceptedOnceThePopulationExists() {
        SamplingShift shift = openShift();
        shift.registerPopulation(population(420, 25, 18));

        assertDoesNotThrow(() -> ShiftValidator.validateCanRegisterBag(shift, executingStudy()));
    }

    @Test
    void rn021_closedShiftDoesNotAcceptData() {
        SamplingShift shift = openShift();
        shift.registerPopulation(population(420, 25, 18));
        shift.close();

        assertThrows(BusinessRuleException.class, () -> ShiftValidator.validateCanRegisterBag(shift, executingStudy()));
    }

    @Test
    void ca023_shiftWithPendingSourceCannotCloseAndNamesTheSource() {
        SamplingShift shift = openShift();
        shift.markSourceWithoutGeneration(1L);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> ShiftValidator.validateCanClose(shift, Map.of(1L, "AU1", 2L, "BA")));
        assertEquals("No se puede cerrar el turno D3-M: faltan registros de las fuentes BA. "
                + "Registre sus bolsas o márquelas como \"sin generación\".", exception.getMessage());
    }

    @Test
    void ca023_shiftCanCloseWhenEverySourceIsCovered() {
        SamplingShift shift = openShift();
        shift.markSourceWithoutGeneration(1L);
        shift.markSourceWithoutGeneration(2L);

        assertDoesNotThrow(() -> ShiftValidator.validateCanClose(shift, Map.of(1L, "AU1", 2L, "BA")));
    }

    @Test
    void rn025_shiftIsReopenedOnlyWhileTheStudyIsExecuting() {
        SamplingShift shift = openShift();
        shift.close();
        CharacterizationStudy cleaning = executingStudy();
        cleaning.changeStatus(StudyStatus.EN_DEPURACION);

        assertDoesNotThrow(() -> ShiftValidator.validateReopen(shift, executingStudy()));
        assertThrows(BusinessRuleException.class, () -> ShiftValidator.validateReopen(shift, cleaning));
    }
}
