package com.ecopoint.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class of the integration tests (spec 002, decision T8). They run against
 * a local PostgreSQL database {@code ecopoint_test}, never against the
 * development database. The database is created if it does not exist, the
 * schema is rebuilt once per test run with {@code sql/001_schema.sql}, and
 * every test starts with empty tables.
 *
 * <p>Connection data comes from environment variables: {@code ECOPOINT_TEST_DB_URL}
 * (default {@code jdbc:postgresql://localhost:5432/ecopoint_test}),
 * {@code ECOPOINT_DB_USER} (default {@code postgres}) and {@code ECOPOINT_DB_PASSWORD}.</p>
 *
 * @author mlanderoc
 * @version 1.0
 */
@SpringBootTest
public abstract class PostgresIntegrationTest {

    /** JDBC URL of the test database. */
    protected static final String TEST_URL =
            env("ECOPOINT_TEST_DB_URL", "jdbc:postgresql://localhost:5432/ecopoint_test");
    /** Database user. */
    protected static final String USER = env("ECOPOINT_DB_USER", "postgres");
    /** Database password, read from the environment only. */
    protected static final String PASSWORD = env("ECOPOINT_DB_PASSWORD", "");

    private static final String ALL_TABLES = "EVIDENCIA, ALERTA, CORRECCION, MEDICION_DENSIDAD, "
            + "RESULTADO_CUARTEO, CUARTEO, BOLSA, POBLACION_TURNO, TURNO_SIN_GENERACION, TURNO, "
            + "FUENTE_ESTUDIO, REAPERTURA_ESTUDIO, DIA_MUESTREO, ESTUDIO, CUADRILLA_INTEGRANTE, CUADRILLA, "
            + "USUARIO, SUBCATEGORIA_RESIDUO, FUENTE_GENERACION, INSTITUCION";

    private static boolean schemaReady;

    @Autowired
    protected JdbcTemplate jdbc;

    @DynamicPropertySource
    static void testDatabase(DynamicPropertyRegistry registry) {
        createDatabaseIfMissing();
        registry.add("spring.datasource.url", () -> TEST_URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    /**
     * Rebuilds the schema once per test run and empties every table before each test.
     */
    @BeforeEach
    void cleanDatabase() {
        synchronized (PostgresIntegrationTest.class) {
            if (!schemaReady) {
                runScript("001_schema.sql");
                schemaReady = true;
            }
        }
        jdbc.execute("TRUNCATE " + ALL_TABLES + " RESTART IDENTITY CASCADE");
    }

    /**
     * Runs one of the SQL scripts of the {@code sql/} folder as a whole. The
     * PostgreSQL driver splits the statements and understands dollar-quoted
     * function bodies.
     *
     * @param fileName script name, e.g. {@code 002_seed.sql}
     */
    protected void runScript(String fileName) {
        jdbc.execute(readScript(fileName));
    }

    /**
     * Reads a script of the {@code sql/} folder.
     *
     * @param fileName script name
     * @return the script text
     */
    protected static String readScript(String fileName) {
        try {
            return Files.readString(Path.of("sql", fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer sql/" + fileName, e);
        }
    }

    private static void createDatabaseIfMissing() {
        int slash = TEST_URL.lastIndexOf('/');
        String serverUrl = TEST_URL.substring(0, slash) + "/postgres";
        String database = TEST_URL.substring(slash + 1).replaceAll("\\?.*$", "");
        try (Connection connection = DriverManager.getConnection(serverUrl, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            var exists = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + database + "'");
            if (!exists.next()) {
                statement.execute("CREATE DATABASE " + database + " ENCODING 'UTF8' TEMPLATE template0");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo conectar a PostgreSQL para las pruebas de integración. "
                    + "Revise que PostgreSQL esté encendido y que ECOPOINT_DB_PASSWORD esté definida.", e);
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
