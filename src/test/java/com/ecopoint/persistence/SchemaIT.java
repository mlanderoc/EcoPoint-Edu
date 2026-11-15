package com.ecopoint.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests of the database scripts and constraints (spec 002:
 * CA-100 to CA-104, CA-106 and the database side of CA-107).
 *
 * @author mlanderoc
 * @version 1.0
 */
class SchemaIT extends PostgresIntegrationTest {

    private static final String CONTENT_HASH = "SELECT md5(string_agg(x, '|' ORDER BY x)) FROM ("
            + "SELECT b::text x FROM BOLSA b UNION ALL SELECT c::text FROM CUARTEO c "
            + "UNION ALL SELECT r::text FROM RESULTADO_CUARTEO r UNION ALL SELECT p::text FROM POBLACION_TURNO p "
            + "UNION ALL SELECT u::text FROM USUARIO u UNION ALL SELECT f::text FROM FUENTE_GENERACION f) s";

    @BeforeEach
    void loadSchemaAndSeed() {
        runScript("001_schema.sql");
        runScript("002_seed.sql");
    }

    private Long anyShiftWithBags() {
        return jdbc.queryForObject("SELECT MIN(id_turno) FROM BOLSA", Long.class);
    }

    @Test
    void ca100_scriptsCreateThePilotInstitutionCatalogUsersAndDemoStudy() {
        assertEquals(1, count("SELECT COUNT(*) FROM INSTITUCION WHERE nombre = 'I.E. Prudencia Daza'"));
        assertEquals(8, count("SELECT COUNT(*) FROM FUENTE_GENERACION"));
        assertEquals(4, count("SELECT COUNT(DISTINCT grupo) FROM SUBCATEGORIA_RESIDUO"));
        assertTrue(count("SELECT COUNT(*) FROM SUBCATEGORIA_RESIDUO") >= 15);
        assertTrue(count("SELECT COUNT(*) FROM USUARIO WHERE rol = 'DOCENTE'") >= 1);
        assertTrue(count("SELECT COUNT(*) FROM USUARIO WHERE rol = 'ESTUDIANTE_UNIVERSITARIO'") >= 1);
        assertEquals(1, count("SELECT COUNT(*) FROM ESTUDIO WHERE estado = 'CERRADO'"));
        assertEquals(16, count("SELECT COUNT(*) FROM TURNO t JOIN ESTUDIO e ON e.id = t.id_estudio WHERE e.estado = 'CERRADO'"));
        assertEquals(0, count("SELECT COUNT(*) FROM V_REGISTROS_PENDIENTES v JOIN ESTUDIO e ON e.id = v.id_estudio "
                + "WHERE e.estado = 'CERRADO'"));
    }

    @Test
    void ca101_runningTheScriptsAgainLeavesTheSameContent() {
        String first = jdbc.queryForObject(CONTENT_HASH, String.class);

        runScript("001_schema.sql");
        runScript("002_seed.sql");

        assertEquals(first, jdbc.queryForObject(CONTENT_HASH, String.class));
    }

    @Test
    void ca102_bagWithTareGreaterThanGrossWeightIsRejectedByTheDatabase() {
        Long shift = anyShiftWithBags();

        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO BOLSA (id_turno, id_fuente, rotulo, peso_bruto, tara, volumen, digitado_por, digitado_en) "
                        + "VALUES (?, 1, 'AU1-D1-M-9', 3.20, 3.50, 0.050, 2, now())", shift));
    }

    @Test
    void ca103_secondQuarteringForTheSameShiftIsRejectedByTheDatabase() {
        Long shift = jdbc.queryForObject("SELECT MIN(id_turno) FROM CUARTEO", Long.class);

        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO CUARTEO (id_turno, peso_neto_turno, peso_submuestra, estado_calidad, digitado_por, digitado_en) "
                        + "VALUES (?, 60, 50, 'CONFORME', 3, now())", shift));
    }

    @Test
    void ca104_sourceWithBagsCannotBeDeleted() {
        assertThrows(DataAccessException.class,
                () -> jdbc.update("DELETE FROM FUENTE_GENERACION WHERE codigo = 'PAE'"));
    }

    @Test
    void ca106_netWeightAndDensityAreCalculatedByTheDatabase() {
        Long shift = anyShiftWithBags();
        Long id = jdbc.queryForObject(
                "INSERT INTO BOLSA (id_turno, id_fuente, rotulo, peso_bruto, tara, volumen, digitado_por, digitado_en) "
                        + "VALUES (?, 1, 'AU1-D1-M-9', 12.40, 0.40, 0.080, 2, now()) RETURNING id", Long.class, shift);

        Map<String, Object> bag = jdbc.queryForMap("SELECT peso_neto, densidad FROM BOLSA WHERE id = ?", id);

        assertEquals(new BigDecimal("12.00"), bag.get("peso_neto"));
        assertEquals(new BigDecimal("150.00"), bag.get("densidad"));
    }

    @Test
    void rn031_invalidBagLabelIsRejectedByTheDatabase() {
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO BOLSA (id_turno, id_fuente, rotulo, peso_bruto, tara, volumen, digitado_por, digitado_en) "
                        + "VALUES (?, 1, 'aula-1', 5, 0.2, 0.05, 2, now())", anyShiftWithBags()));
    }

    @Test
    void ca107_correctionHistoryCannotBeUpdatedOrDeleted() {
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE CORRECCION SET motivo = 'otro'"));
        assertThrows(DataAccessException.class, () -> jdbc.update("DELETE FROM CORRECCION"));
        assertEquals(1, count("SELECT COUNT(*) FROM CORRECCION"));
    }

    @Test
    void r016_cancelledStudyKeepsWhoWhenAndWhy() {
        Long study = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'CERRADO'", Long.class);

        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE ESTUDIO SET estado = 'CANCELADO' WHERE id = ?", study));
        jdbc.update("UPDATE ESTUDIO SET estado = 'CANCELADO', cancelado_por = 1, cancelado_en = now(), "
                + "motivo_cancelacion = 'Fechas equivocadas' WHERE id = ?", study);

        assertEquals(1, count("SELECT COUNT(*) FROM ESTUDIO WHERE estado = 'CANCELADO'"));
        assertTrue(count("SELECT COUNT(*) FROM BOLSA") > 0);
    }

    @Test
    void rn019_plannedStudyIsDeletedWithItsDaysAndSources() {
        Long study = jdbc.queryForObject("INSERT INTO ESTUDIO (id_institucion) VALUES (1) RETURNING id", Long.class);
        jdbc.update("INSERT INTO DIA_MUESTREO (id_estudio, numero_dia, fecha) "
                + "SELECT ?, n, DATE '2030-03-01' + n FROM generate_series(1, 8) n", study);
        jdbc.update("INSERT INTO FUENTE_ESTUDIO (id_estudio, id_fuente) SELECT ?, id FROM FUENTE_GENERACION", study);

        jdbc.update("DELETE FROM ESTUDIO WHERE id = ?", study);

        assertEquals(0, count("SELECT COUNT(*) FROM DIA_MUESTREO WHERE id_estudio = " + study));
        assertEquals(0, count("SELECT COUNT(*) FROM FUENTE_ESTUDIO WHERE id_estudio = " + study));
    }

    @Test
    void rn019_studyWithShiftsCannotBeDeleted() {
        assertThrows(DataAccessException.class,
                () -> jdbc.update("DELETE FROM ESTUDIO WHERE estado = 'CERRADO'"));
    }

    @Test
    void r016_updateScriptAddsTheChangesToAnOldDatabaseWithoutLosingData() {
        jdbc.execute("ALTER TABLE ESTUDIO DROP CONSTRAINT ck_estudio_cancelacion");
        jdbc.execute("ALTER TABLE ESTUDIO DROP COLUMN cancelado_por, DROP COLUMN cancelado_en, "
                + "DROP COLUMN motivo_cancelacion");
        jdbc.execute("ALTER TABLE INSTITUCION DROP COLUMN activa");
        String before = jdbc.queryForObject(CONTENT_HASH, String.class);

        runScript("003_ajustes_d13.sql");
        runScript("003_ajustes_d13.sql");

        assertEquals(before, jdbc.queryForObject(CONTENT_HASH, String.class));
        assertEquals(1, count("SELECT COUNT(*) FROM INSTITUCION WHERE activa"));
        assertEquals(1, count("SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_name = 'estudio' AND column_name = 'motivo_cancelacion'"));
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
