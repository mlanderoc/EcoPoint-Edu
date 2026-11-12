package com.ecopoint.persistence;

import com.ecopoint.EcoPointApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CA-108: the application connects to another database only by changing the
 * {@code ECOPOINT_DB_URL} setting, without recompiling. The test starts the
 * real application twice, each time with a different value of that setting.
 *
 * @author mlanderoc
 * @version 1.0
 */
class ConfigurationIT {

    private static final String SERVER = PostgresIntegrationTest.TEST_URL
            .substring(0, PostgresIntegrationTest.TEST_URL.lastIndexOf('/'));

    private static String currentDatabase(String databaseName) throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EcoPointApplication.class)
                .properties("ECOPOINT_DB_URL=" + SERVER + "/" + databaseName,
                        "ECOPOINT_DB_USER=" + PostgresIntegrationTest.USER,
                        "ECOPOINT_DB_PASSWORD=" + PostgresIntegrationTest.PASSWORD)
                .run()) {
            return context.getBean(JdbcTemplate.class).queryForObject("SELECT current_database()", String.class);
        }
    }

    private static void createDatabaseIfMissing(String databaseName) throws Exception {
        try (Connection connection = DriverManager.getConnection(SERVER + "/postgres",
                PostgresIntegrationTest.USER, PostgresIntegrationTest.PASSWORD);
             Statement statement = connection.createStatement()) {
            if (!statement.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'").next()) {
                statement.execute("CREATE DATABASE " + databaseName + " ENCODING 'UTF8' TEMPLATE template0");
            }
        }
    }

    @Test
    void ca108_applicationConnectsToAnotherDatabaseOnlyByChangingTheSetting() throws Exception {
        createDatabaseIfMissing("ecopoint_test");
        createDatabaseIfMissing("ecopoint_test_alterna");

        assertEquals("ecopoint_test", currentDatabase("ecopoint_test"));
        assertEquals("ecopoint_test_alterna", currentDatabase("ecopoint_test_alterna"));
    }
}
