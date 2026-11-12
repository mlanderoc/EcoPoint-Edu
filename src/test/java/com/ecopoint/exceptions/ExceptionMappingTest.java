package com.ecopoint.exceptions;

import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Permission;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.SourceType;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteBag;
import com.ecopoint.model.WasteGroup;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.GenerationSourceValidator;
import com.ecopoint.validation.QuarteringValidator;
import com.ecopoint.validation.ShiftValidator;
import com.ecopoint.validation.StudyValidator;
import com.ecopoint.validation.VerificationValidator;
import com.ecopoint.validation.WasteBagValidator;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CA-090: every rejection of the domain rules throws an exception of the
 * {@link EcoPointException} hierarchy, of the right type and error code, with
 * a message in Spanish that explains what failed.
 *
 * @author mlanderoc
 * @version 1.0
 */
class ExceptionMappingTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 3, 4, 12, 0);
    private static final Pattern ENGLISH_WORDS =
            Pattern.compile("\\b(the|must|cannot|should|invalid|not found|null)\\b", Pattern.CASE_INSENSITIVE);

    private static User student() {
        return new User(2L, "Luis", "Gómez", "luis@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
    }

    private static CharacterizationStudy studyInStatus(StudyStatus... path) {
        CharacterizationStudy study = new CharacterizationStudy(1L, 1L);
        for (StudyStatus status : path) {
            study.changeStatus(status);
        }
        return study;
    }

    private static WasteBag bag(String gross, String tare) {
        return new WasteBag(null, 1L, "AU", new BigDecimal(gross), new BigDecimal(tare), new BigDecimal("0.080"),
                new VerificationInfo(1L, AT));
    }

    private static Stream<Arguments> rejections() {
        return Stream.of(
                Arguments.of("Código de fuente en minúsculas (CA-001)", InvalidDataException.class,
                        "ERR_INVALID_DATA", (Executable) () -> GenerationSourceValidator.validate(
                                new GenerationSource(null, 1L, "au", "Aulas", SourceType.AULAS))),
                Arguments.of("Tara mayor que el peso bruto (CA-031)", InvalidDataException.class,
                        "ERR_INVALID_DATA", (Executable) () -> WasteBagValidator.validate(bag("3.20", "3.50"), Set.of(1L))),
                Arguments.of("Población de cero personas (CA-021)", InvalidDataException.class,
                        "ERR_INVALID_DATA", (Executable) () -> ShiftValidator.validatePopulation(
                                new ShiftPopulation(null, 0, 0, 0, new VerificationInfo(1L, AT)))),
                Arguments.of("Código de fuente repetido (CA-002)", BusinessRuleException.class,
                        "ERR_BUSINESS_RULE", (Executable) () -> GenerationSourceValidator.validateUniqueCode(
                                new GenerationSource(null, 1L, "AU", "Aulas", SourceType.AULAS), Set.of("AU"))),
                Arguments.of("Bolsa sin población registrada (CA-022)", BusinessRuleException.class,
                        "ERR_BUSINESS_RULE", (Executable) () -> ShiftValidator.validateCanRegisterBag(
                                new SamplingShift(1L, 1L, 3, ShiftType.MANANA), studyInStatus(StudyStatus.EN_EJECUCION))),
                Arguments.of("Saltar de planificado a cerrado (CA-013)", BusinessRuleException.class,
                        "ERR_BUSINESS_RULE", (Executable) () -> studyInStatus().changeStatus(StudyStatus.CERRADO)),
                Arguments.of("Submuestra fuera de rango (CA-050)", BusinessRuleException.class,
                        "ERR_BUSINESS_RULE", (Executable) () -> {
                            Quartering quartering = new Quartering(null, 1L, new BigDecimal("120"),
                                    new BigDecimal("40"), new VerificationInfo(1L, AT));
                            quartering.addResult(new QuarteringResult(1L, "Papel", WasteGroup.BLANCO, BigDecimal.TEN));
                            QuarteringValidator.validate(quartering, CharacterizationSettings.defaults());
                        }),
                Arguments.of("Verificar un registro propio (CA-060)", BusinessRuleException.class,
                        "ERR_BUSINESS_RULE", (Executable) () -> VerificationValidator.validateVerifier(
                                new WasteBag(null, 1L, "AU", BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("0.05"),
                                        new VerificationInfo(student().getId(), AT)), student())),
                Arguments.of("Cerrar estudio con cuarteo no conforme (RN-016)", QualityControlException.class,
                        "ERR_QUALITY_CONTROL", (Executable) () -> StudyValidator.validateClose(
                                studyInStatus(StudyStatus.EN_EJECUCION, StudyStatus.EN_DEPURACION), 0, 1)),
                Arguments.of("Estudiante abre o cierra turnos (CA-204)", AccessDeniedException.class,
                        "ERR_FORBIDDEN", (Executable) () -> AccessPolicy.require(student(), Permission.MANAGE_SHIFTS)),
                Arguments.of("Registro inexistente (lo lanza la capa de servicios)", ResourceNotFoundException.class,
                        "ERR_NOT_FOUND", (Executable) () -> {
                            throw new ResourceNotFoundException("Estudio", 99L);
                        })
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rejections")
    void ca090_everyRejectionThrowsTheRightEcoPointExceptionInSpanish(
            String rule, Class<? extends EcoPointException> expectedType, String expectedCode, Executable action) {
        EcoPointException exception = assertThrows(expectedType, action);

        assertInstanceOf(EcoPointException.class, exception);
        assertEquals(expectedCode, exception.getErrorCode());
        assertTrue(exception.getMessage() != null && !exception.getMessage().isBlank(), rule);
        assertFalse(ENGLISH_WORDS.matcher(exception.getMessage()).find(),
                "El mensaje debe estar en español: " + exception.getMessage());
    }
}
