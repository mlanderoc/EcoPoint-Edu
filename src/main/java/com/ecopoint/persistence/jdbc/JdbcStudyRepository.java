package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.SamplingDay;
import com.ecopoint.model.StudyCancellation;
import com.ecopoint.model.StudyReopening;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.persistence.StudyRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * PostgreSQL implementation of {@link StudyRepository} (tables {@code ESTUDIO},
 * {@code DIA_MUESTREO}, {@code REAPERTURA_ESTUDIO} and {@code FUENTE_ESTUDIO}).
 * A study and its days are saved in one transaction (R-020).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Repository
public class JdbcStudyRepository implements StudyRepository {

    private static final String COLUMNS =
            "id, id_institucion, estado, cancelado_por, cancelado_en, motivo_cancelacion";

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcStudyRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public CharacterizationStudy save(CharacterizationStudy study) {
        DataAccessTranslator.execute("guardar el estudio de caracterización", () -> {
            if (study.getId() == null) {
                study.setId(jdbc.queryForObject("INSERT INTO ESTUDIO (id_institucion, estado) "
                                + "VALUES (:institucion, CAST(:estado AS estado_estudio)) RETURNING id",
                        new MapSqlParameterSource("institucion", study.getInstitutionId())
                                .addValue("estado", study.getStatus().name()), Long.class));
                for (SamplingDay day : study.getDays()) {
                    jdbc.update("INSERT INTO DIA_MUESTREO (id_estudio, numero_dia, fecha) VALUES (:estudio, :dia, :fecha)",
                            dayParameters(study.getId(), day));
                }
            } else {
                jdbc.update("UPDATE ESTUDIO SET estado = CAST(:estado AS estado_estudio), cancelado_por = :canceladoPor, "
                                + "cancelado_en = :canceladoEn, motivo_cancelacion = :motivo WHERE id = :id",
                        cancellationParameters(study).addValue("id", study.getId())
                                .addValue("estado", study.getStatus().name()));
                // First clear the dates, so moving a date to another day never hits the UNIQUE (id_estudio, fecha).
                jdbc.update("UPDATE DIA_MUESTREO SET fecha = NULL WHERE id_estudio = :estudio",
                        new MapSqlParameterSource("estudio", study.getId()));
                for (SamplingDay day : study.getDays()) {
                    jdbc.update("UPDATE DIA_MUESTREO SET fecha = :fecha WHERE id_estudio = :estudio AND numero_dia = :dia",
                            dayParameters(study.getId(), day));
                }
            }
            saveNewReopenings(study);
        });
        return study;
    }

    @Override
    public Optional<CharacterizationStudy> findById(Long id) {
        return DataAccessTranslator.execute("buscar el estudio " + id,
                () -> jdbc.queryForList("SELECT " + COLUMNS + " FROM ESTUDIO WHERE id = :id",
                        new MapSqlParameterSource("id", id)).stream().findFirst().map(this::toStudy));
    }

    @Override
    public List<CharacterizationStudy> findByInstitution(Long institutionId) {
        return DataAccessTranslator.execute("listar los estudios de la institución " + institutionId,
                () -> jdbc.queryForList("SELECT " + COLUMNS + " FROM ESTUDIO "
                                + "WHERE id_institucion = :institucion ORDER BY id DESC",
                        new MapSqlParameterSource("institucion", institutionId)).stream().map(this::toStudy).toList());
    }

    @Override
    public void updateStatus(Long studyId, StudyStatus status) {
        DataAccessTranslator.execute("cambiar el estado del estudio " + studyId,
                () -> jdbc.update("UPDATE ESTUDIO SET estado = CAST(:estado AS estado_estudio) WHERE id = :id",
                        new MapSqlParameterSource("id", studyId).addValue("estado", status.name())));
    }

    @Override
    public void delete(Long studyId) {
        DataAccessTranslator.execute("eliminar el estudio " + studyId,
                () -> jdbc.update("DELETE FROM ESTUDIO WHERE id = :id", new MapSqlParameterSource("id", studyId)));
    }

    @Override
    @Transactional
    public void saveParticipatingSources(Long studyId, Set<Long> sourceIds) {
        DataAccessTranslator.execute("guardar las fuentes participantes del estudio " + studyId, () -> {
            jdbc.update("DELETE FROM FUENTE_ESTUDIO WHERE id_estudio = :estudio",
                    new MapSqlParameterSource("estudio", studyId));
            for (Long sourceId : sourceIds) {
                jdbc.update("INSERT INTO FUENTE_ESTUDIO (id_estudio, id_fuente) VALUES (:estudio, :fuente)",
                        new MapSqlParameterSource("estudio", studyId).addValue("fuente", sourceId));
            }
        });
    }

    @Override
    public Set<Long> findParticipatingSources(Long studyId) {
        return DataAccessTranslator.execute("consultar las fuentes participantes del estudio " + studyId,
                () -> new LinkedHashSet<>(jdbc.queryForList("SELECT id_fuente FROM FUENTE_ESTUDIO "
                        + "WHERE id_estudio = :estudio ORDER BY id_fuente",
                        new MapSqlParameterSource("estudio", studyId), Long.class)));
    }

    @Override
    public int countPendingRecords(Long studyId) {
        return count("contar los registros pendientes del estudio " + studyId,
                "SELECT COUNT(*) FROM V_REGISTROS_PENDIENTES WHERE id_estudio = :estudio", studyId);
    }

    @Override
    public int countUnresolvedQuarterings(Long studyId) {
        return count("contar los cuarteos no conformes del estudio " + studyId,
                "SELECT COUNT(*) FROM CUARTEO c JOIN TURNO t ON t.id = c.id_turno "
                        + "WHERE t.id_estudio = :estudio AND c.estado_calidad = 'NO_CONFORME'", studyId);
    }

    private int count(String operation, String sql, Long studyId) {
        Integer value = DataAccessTranslator.execute(operation,
                () -> jdbc.queryForObject(sql, new MapSqlParameterSource("estudio", studyId), Integer.class));
        return value == null ? 0 : value;
    }

    private void saveNewReopenings(CharacterizationStudy study) {
        Integer stored = jdbc.queryForObject("SELECT COUNT(*) FROM REAPERTURA_ESTUDIO WHERE id_estudio = :estudio",
                new MapSqlParameterSource("estudio", study.getId()), Integer.class);
        List<StudyReopening> reopenings = study.getReopenings();
        for (int i = stored == null ? 0 : stored; i < reopenings.size(); i++) {
            StudyReopening reopening = reopenings.get(i);
            jdbc.update("INSERT INTO REAPERTURA_ESTUDIO (id_estudio, id_usuario, motivo, reabierto_en) "
                            + "VALUES (:estudio, :usuario, :motivo, :fecha)",
                    new MapSqlParameterSource("estudio", study.getId())
                            .addValue("usuario", reopening.userId())
                            .addValue("motivo", reopening.reason())
                            .addValue("fecha", Timestamp.valueOf(reopening.at())));
        }
    }

    private CharacterizationStudy toStudy(Map<String, Object> row) {
        Long id = ((Number) row.get("id")).longValue();
        MapSqlParameterSource byStudy = new MapSqlParameterSource("estudio", id);
        List<LocalDate> dates = new ArrayList<>(jdbc.query(
                "SELECT fecha FROM DIA_MUESTREO WHERE id_estudio = :estudio ORDER BY numero_dia", byStudy,
                (rs, n) -> rs.getDate("fecha") == null ? null : rs.getDate("fecha").toLocalDate()));
        List<StudyReopening> reopenings = jdbc.query(
                "SELECT id_usuario, motivo, reabierto_en FROM REAPERTURA_ESTUDIO WHERE id_estudio = :estudio ORDER BY id",
                byStudy, (rs, n) -> new StudyReopening(rs.getLong("id_usuario"), rs.getString("motivo"),
                        rs.getTimestamp("reabierto_en").toLocalDateTime()));
        return CharacterizationStudy.restore(id, ((Number) row.get("id_institucion")).longValue(),
                StudyStatus.valueOf(row.get("estado").toString()), dates, reopenings, cancellationOf(row));
    }

    private static StudyCancellation cancellationOf(Map<String, Object> row) {
        if (row.get("cancelado_por") == null) {
            return null;
        }
        return new StudyCancellation(((Number) row.get("cancelado_por")).longValue(),
                row.get("motivo_cancelacion").toString(), ((Timestamp) row.get("cancelado_en")).toLocalDateTime());
    }

    private static MapSqlParameterSource cancellationParameters(CharacterizationStudy study) {
        StudyCancellation cancellation = study.getCancellation();
        return new MapSqlParameterSource()
                .addValue("canceladoPor", cancellation == null ? null : cancellation.userId())
                .addValue("canceladoEn", cancellation == null ? null : Timestamp.valueOf(cancellation.at()))
                .addValue("motivo", cancellation == null ? null : cancellation.reason());
    }

    private static MapSqlParameterSource dayParameters(Long studyId, SamplingDay day) {
        return new MapSqlParameterSource("estudio", studyId)
                .addValue("dia", day.getDayNumber())
                .addValue("fecha", day.getDate() == null ? null : Date.valueOf(day.getDate()));
    }
}
