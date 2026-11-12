package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.persistence.jdbc.DataAccessTranslator;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test of {@link DataAccessTranslator} against PostgreSQL (spec 002, R-021).
 *
 * @author mlanderoc
 * @version 1.0
 */
class DataAccessTranslatorIT extends PostgresIntegrationTest {

    @Test
    void r021_databaseErrorBecomesPersistenceExceptionWithoutSqlDetails() {
        PersistenceException exception = assertThrows(PersistenceException.class,
                () -> DataAccessTranslator.execute("consultar las fuentes",
                        () -> jdbc.queryForObject("SELECT * FROM TABLA_QUE_NO_EXISTE", Integer.class)));

        assertEquals("ERR_PERSISTENCE", exception.getErrorCode());
        assertEquals("Error de persistencia durante la operación [consultar las fuentes].", exception.getMessage());
        assertFalse(exception.getMessage().contains("SELECT"));
        assertInstanceOf(DataAccessException.class, exception.getCause());
    }

    @Test
    void r021_successfulOperationReturnsItsResult() {
        Integer one = DataAccessTranslator.execute("probar la conexión",
                () -> jdbc.queryForObject("SELECT 1", Integer.class));

        assertEquals(1, one);
    }

    @Test
    void r021_operationWithoutResultIsAlsoTranslated() {
        assertThrows(PersistenceException.class,
                () -> DataAccessTranslator.execute("borrar datos", () -> jdbc.update("DELETE FROM TABLA_QUE_NO_EXISTE")));
    }
}
