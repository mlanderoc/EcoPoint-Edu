package com.ecopoint.persistence.jdbc;

import com.ecopoint.persistence.UserActivity;
import com.ecopoint.persistence.UserActivityRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PostgreSQL implementation of {@link UserActivityRepository}. One query
 * counts, by user, the records typed (digitado_por) and verified
 * (verificado_por) in POBLACION_TURNO, BOLSA, CUARTEO and MEDICION_DENSIDAD,
 * and the corrections made to the records each user typed (decision T7).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Repository
public class JdbcUserActivityRepository implements UserActivityRepository {

    /** Every typed record with its kind, as CORRECCION names it in tipo_registro. */
    private static final String RECORDS = "SELECT 'P' AS tipo, 'Población del turno' AS nombre, id, digitado_por, "
            + "verificado_por FROM POBLACION_TURNO "
            + "UNION ALL SELECT 'B', 'Bolsa', id, digitado_por, verificado_por FROM BOLSA "
            + "UNION ALL SELECT 'C', 'Cuarteo', id, digitado_por, verificado_por FROM CUARTEO "
            + "UNION ALL SELECT 'D', 'Medición de densidad', id, digitado_por, verificado_por FROM MEDICION_DENSIDAD";

    private static final String ACTIVITY = "WITH registros AS (" + RECORDS + ") "
            + "SELECT u.id, "
            + "(SELECT COUNT(*) FROM registros r WHERE r.digitado_por = u.id AND r.tipo = 'P') AS poblaciones, "
            + "(SELECT COUNT(*) FROM registros r WHERE r.digitado_por = u.id AND r.tipo = 'B') AS bolsas, "
            + "(SELECT COUNT(*) FROM registros r WHERE r.digitado_por = u.id AND r.tipo = 'C') AS cuarteos, "
            + "(SELECT COUNT(*) FROM registros r WHERE r.digitado_por = u.id AND r.tipo = 'D') AS densidades, "
            + "(SELECT COUNT(*) FROM registros r WHERE r.verificado_por = u.id) AS verificaciones, "
            + "(SELECT COUNT(DISTINCT (c.tipo_registro, c.id_registro, c.corregido_en)) FROM CORRECCION c "
            + "  JOIN registros r ON r.nombre = c.tipo_registro AND r.id = c.id_registro "
            + "  WHERE r.digitado_por = u.id) AS correcciones "
            + "FROM USUARIO u ";

    private static final RowMapper<UserActivity> MAPPER = (rs, row) -> new UserActivity(rs.getLong("id"),
            rs.getInt("poblaciones"), rs.getInt("bolsas"), rs.getInt("cuarteos"), rs.getInt("densidades"),
            rs.getInt("verificaciones"), rs.getInt("correcciones"));

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcUserActivityRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserActivity findByUser(Long userId) {
        return DataAccessTranslator.execute("contar los registros del usuario " + userId,
                () -> jdbc.query(ACTIVITY + "WHERE u.id = :id", new MapSqlParameterSource("id", userId), MAPPER)
                        .stream().findFirst().orElse(UserActivity.none(userId)));
    }

    @Override
    public Map<Long, UserActivity> findAll() {
        return DataAccessTranslator.execute("contar los registros de los usuarios", () -> {
            Map<Long, UserActivity> byUser = new LinkedHashMap<>();
            jdbc.query(ACTIVITY + "ORDER BY u.id", MAPPER).forEach(activity -> byUser.put(activity.userId(), activity));
            return byUser;
        });
    }
}
