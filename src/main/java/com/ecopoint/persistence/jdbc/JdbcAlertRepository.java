package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.Alert;
import com.ecopoint.model.AlertType;
import com.ecopoint.persistence.AlertRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL implementation of {@link AlertRepository} (table {@code ALERTA}).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Repository
public class JdbcAlertRepository implements AlertRepository {

    private static final String SELECT = "SELECT id, tipo, tipo_registro, id_registro, descripcion, creada_en, atendida, "
            + "atendida_por, atendida_en, observacion_atencion FROM ALERTA ";

    private static final RowMapper<Alert> MAPPER = (rs, row) -> {
        long target = rs.getLong("id_registro");
        Long targetId = rs.wasNull() ? null : target;
        long attendedBy = rs.getLong("atendida_por");
        Long attender = rs.wasNull() ? null : attendedBy;
        return Alert.restore(rs.getLong("id"), AlertType.valueOf(rs.getString("tipo")), rs.getString("tipo_registro"),
                targetId, rs.getString("descripcion"), FieldRecordMappers.toLocal(rs.getTimestamp("creada_en")),
                rs.getBoolean("atendida"), attender, FieldRecordMappers.toLocal(rs.getTimestamp("atendida_en")),
                rs.getString("observacion_atencion"));
    };

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcAlertRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Alert save(Alert alert) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tipo", alert.getType().name())
                .addValue("tipoRegistro", alert.getTargetType())
                .addValue("registro", alert.getTargetId())
                .addValue("descripcion", alert.getDescription())
                .addValue("creada", FieldRecordMappers.toTimestamp(alert.getCreatedAt()))
                .addValue("atendida", alert.isAttended())
                .addValue("atendidaPor", alert.getAttendedBy())
                .addValue("atendidaEn", FieldRecordMappers.toTimestamp(alert.getAttendedAt()))
                .addValue("observacion", alert.getAttendNote());
        if (alert.getId() == null) {
            alert.setId(DataAccessTranslator.execute("guardar la alerta " + alert.getType().getDisplayName(),
                    () -> jdbc.queryForObject("INSERT INTO ALERTA (tipo, tipo_registro, id_registro, descripcion, creada_en, "
                            + "atendida, atendida_por, atendida_en, observacion_atencion) VALUES (CAST(:tipo AS tipo_alerta), "
                            + ":tipoRegistro, :registro, :descripcion, :creada, :atendida, :atendidaPor, :atendidaEn, "
                            + ":observacion) RETURNING id", params, Long.class)));
        } else {
            DataAccessTranslator.execute("guardar la atención de la alerta " + alert.getId(),
                    () -> jdbc.update("UPDATE ALERTA SET atendida = :atendida, atendida_por = :atendidaPor, "
                            + "atendida_en = :atendidaEn, observacion_atencion = :observacion WHERE id = :id",
                            params.addValue("id", alert.getId())));
        }
        return alert;
    }

    @Override
    public Optional<Alert> findById(Long id) {
        return DataAccessTranslator.execute("buscar la alerta " + id,
                () -> jdbc.query(SELECT + "WHERE id = :id", new MapSqlParameterSource("id", id), MAPPER).stream().findFirst());
    }

    @Override
    public List<Alert> findPending() {
        return DataAccessTranslator.execute("listar las alertas pendientes",
                () -> jdbc.query(SELECT + "WHERE NOT atendida ORDER BY creada_en DESC, id DESC", MAPPER));
    }

    @Override
    public List<Alert> findAll() {
        return DataAccessTranslator.execute("listar las alertas",
                () -> jdbc.query(SELECT + "ORDER BY creada_en DESC, id DESC", MAPPER));
    }
}
