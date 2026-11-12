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
    void ca090_qualityControlExceptionHasItsOwnErrorCodeAndSpanishMessage() {
        QualityControlException exception = new QualityControlException(
                "El cuarteo del turno D3-M no es conforme: diferencia de 3,00 %.");

        assertEquals("ERR_QUALITY_CONTROL", exception.getErrorCode());
        assertEquals("El cuarteo del turno D3-M no es conforme: diferencia de 3,00 %.",
                exception.getMessage());
    }

    @Test
    void ca090_qualityControlExceptionIsABusinessRuleException() {
        QualityControlException exception = new QualityControlException("Cuarteo no conforme.");

        assertInstanceOf(BusinessRuleException.class, exception);
        assertInstanceOf(EcoPointException.class, exception);
    }

    @Test
    void ca090_accessDeniedExceptionHasForbiddenErrorCodeAndSpanishMessage() {
        AccessDeniedException exception = new AccessDeniedException("No tiene permiso.");

        assertEquals("ERR_FORBIDDEN", exception.getErrorCode());
        assertEquals("No tiene permiso.", exception.getMessage());
    }

    @Test
    void ca090_accessDeniedExceptionBuildsMessageFromRoleAndAction() {
        AccessDeniedException exception =
                new AccessDeniedException("Estudiante universitario", "abrir, cerrar y reabrir turnos");

        assertEquals("El rol Estudiante universitario no tiene permiso para abrir, cerrar y reabrir turnos.",
                exception.getMessage());
        assertInstanceOf(EcoPointException.class, exception);
    }

    @Test
    void ca205_invalidDataExceptionKeepsTheReasonWithoutTheFieldPrefix() {
        InvalidDataException exception = new InvalidDataException("tara", "La tara no puede superar el peso bruto.");

        assertEquals("tara", exception.getFieldName());
        assertEquals("La tara no puede superar el peso bruto.", exception.getProblemDescription());
        assertEquals("Dato inválido en el campo 'tara': La tara no puede superar el peso bruto.", exception.getMessage());
    }

    @Test
    void ca205_generalInvalidDataExceptionUsesTheWholeMessageAsReason() {
        InvalidDataException exception = new InvalidDataException("El archivo no es un GeoJSON válido.");

        assertEquals("general", exception.getFieldName());
        assertEquals("El archivo no es un GeoJSON válido.", exception.getProblemDescription());
    }

    @Test
    void ca202_unauthorizedExceptionHasItsOwnErrorCodeAndSpanishMessage() {
        UnauthorizedException exception = new UnauthorizedException("Correo o contraseña incorrectos.");

        assertInstanceOf(EcoPointException.class, exception);
        assertEquals("ERR_UNAUTHORIZED", exception.getErrorCode());
        assertEquals("Correo o contraseña incorrectos.", exception.getMessage());
    }
}
