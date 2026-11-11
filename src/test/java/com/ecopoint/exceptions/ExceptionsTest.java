package com.ecopoint.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Unit tests for the exceptions added by spec 001
 * ({@link QualityControlException} and {@link AccessDeniedException}).
 *
 * @author mlanderoc
 * @version 1.0
 */
class ExceptionsTest {

    @Test
    void qualityControlExceptionHasItsOwnErrorCodeAndSpanishMessage() {
        QualityControlException exception = new QualityControlException(
                "El cuarteo del turno D3-M no es conforme: diferencia de 3,00 %.");

        assertEquals("ERR_QUALITY_CONTROL", exception.getErrorCode());
        assertEquals("El cuarteo del turno D3-M no es conforme: diferencia de 3,00 %.",
                exception.getMessage());
    }

    @Test
    void qualityControlExceptionIsABusinessRuleException() {
        QualityControlException exception = new QualityControlException("Cuarteo no conforme.");

        assertInstanceOf(BusinessRuleException.class, exception);
        assertInstanceOf(EcoPointException.class, exception);
    }

    @Test
    void accessDeniedExceptionHasForbiddenErrorCodeAndSpanishMessage() {
        AccessDeniedException exception = new AccessDeniedException("No tiene permiso.");

        assertEquals("ERR_FORBIDDEN", exception.getErrorCode());
        assertEquals("No tiene permiso.", exception.getMessage());
    }

    @Test
    void accessDeniedExceptionBuildsMessageFromRoleAndAction() {
        AccessDeniedException exception =
                new AccessDeniedException("Estudiante de colegio", "verificar registros");

        assertEquals("El rol Estudiante de colegio no tiene permiso para verificar registros.",
                exception.getMessage());
        assertInstanceOf(EcoPointException.class, exception);
    }
}
