package com.ecopoint.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests of {@link UserActivityRepository} with the demo data
 * (spec 003, R-133; decision T7 of the D13 plan).
 *
 * @author mlanderoc
 * @version 1.0
 */
class UserActivityRepositoryIT extends PostgresIntegrationTest {

    @Autowired
    private UserActivityRepository repository;

    @BeforeEach
    void loadDemoData() {
        runScript("002_seed.sql");
    }

    @Test
    void r133_totalsOfEachUserMatchAManualCount() {
        Map<Long, UserActivity> all = repository.findAll();

        assertEquals(count("SELECT COUNT(*) FROM USUARIO"), all.size());
        for (UserActivity activity : all.values()) {
            Long user = activity.userId();
            assertEquals(count("SELECT COUNT(*) FROM BOLSA WHERE digitado_por = " + user), activity.bags());
            assertEquals(count("SELECT COUNT(*) FROM POBLACION_TURNO WHERE digitado_por = " + user), activity.populations());
            assertEquals(count("SELECT COUNT(*) FROM CUARTEO WHERE digitado_por = " + user), activity.quarterings());
            assertEquals(count("SELECT COUNT(*) FROM MEDICION_DENSIDAD WHERE digitado_por = " + user), activity.densities());
            assertEquals(count("SELECT (SELECT COUNT(*) FROM BOLSA WHERE verificado_por = " + user + ") "
                    + "+ (SELECT COUNT(*) FROM POBLACION_TURNO WHERE verificado_por = " + user + ") "
                    + "+ (SELECT COUNT(*) FROM CUARTEO WHERE verificado_por = " + user + ") "
                    + "+ (SELECT COUNT(*) FROM MEDICION_DENSIDAD WHERE verificado_por = " + user + ")"),
                    activity.verifications());
            assertEquals(activity, repository.findByUser(user));
        }
        assertTrue(all.values().stream().mapToInt(UserActivity::typedRecords).sum() > 0);
        assertEquals(1, all.values().stream().mapToInt(UserActivity::correctionsReceived).sum());
    }

    @Test
    void r133_unknownUserHasNoRecords() {
        assertEquals(UserActivity.none(999_999L), repository.findByUser(999_999L));
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
