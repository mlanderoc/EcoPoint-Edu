package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.ShiftStatus;
import com.ecopoint.model.ShiftType;
import com.ecopoint.persistence.DensityRepository;
import com.ecopoint.persistence.QuarteringRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.WasteBagRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PostgreSQL implementation of {@link ShiftRepository} (tables {@code TURNO},
 * {@code POBLACION_TURNO} and {@code TURNO_SIN_GENERACION}). Reading a shift
 * rebuilds the whole aggregate, using the bag, quartering and density
 * repositories for their parts.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Repository
public class JdbcShiftRepository implements ShiftRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final WasteBagRepository bags;
    private final QuarteringRepository quarterings;
    private final DensityRepository densities;

    /**
     * Creates the repository.
     *
     * @param jdbc        Spring JDBC template with named parameters
     * @param bags        bag repository
     * @param quarterings quartering repository
     * @param densities   density repository
     */
    public JdbcShiftRepository(NamedParameterJdbcTemplate jdbc, WasteBagRepository bags,
                               QuarteringRepository quarterings, DensityRepository densities) {
        this.jdbc = jdbc;
        this.bags = bags;
        this.quarterings = quarterings;
        this.densities = densities;
    }

    @Override
    @Transactional
    public SamplingShift save(SamplingShift shift) {
        DataAccessTranslator.execute("guardar el turno " + shift.getCode(), () -> {
            if (shift.getId() == null) {
                shift.setId(jdbc.queryForObject("INSERT INTO TURNO (id_estudio, numero_dia, tipo, estado, id_cuadrilla, cerrado_en) "
                                + "VALUES (:estudio, :dia, CAST(:tipo AS tipo_turno), CAST(:estado AS estado_turno), :cuadrilla, "
                                + "CASE WHEN :estado = 'CERRADO' THEN now() END) RETURNING id",
                        new MapSqlParameterSource("estudio", shift.getStudyId())
                                .addValue("dia", shift.getDayNumber())
                                .addValue("tipo", shift.getType().name())
                                .addValue("estado", shift.getStatus().name())
                                .addValue("cuadrilla", shift.getCrewId()), Long.class));
            } else {
                writeStatus(shift.getId(), shift.getStatus());
                jdbc.update("UPDATE TURNO SET id_cuadrilla = :cuadrilla WHERE id = :id",
                        new MapSqlParameterSource("id", shift.getId()).addValue("cuadrilla", shift.getCrewId()));
            }
            savePopulation(shift);
            MapSqlParameterSource byShift = new MapSqlParameterSource("turno", shift.getId());
            jdbc.update("DELETE FROM TURNO_SIN_GENERACION WHERE id_turno = :turno", byShift);
            for (Long sourceId : shift.getSourcesWithoutGeneration()) {
                jdbc.update("INSERT INTO TURNO_SIN_GENERACION (id_turno, id_fuente) VALUES (:turno, :fuente)",
                        new MapSqlParameterSource("turno", shift.getId()).addValue("fuente", sourceId));
            }
        });
        return shift;
    }

    @Override
    public Optional<SamplingShift> findById(Long id) {
        return DataAccessTranslator.execute("buscar el turno " + id,
                () -> jdbc.queryForList("SELECT id, id_estudio, numero_dia, tipo, estado, id_cuadrilla FROM TURNO WHERE id = :id",
                        new MapSqlParameterSource("id", id)).stream().findFirst().map(this::toShift));
    }

    @Override
    public List<SamplingShift> findByStudy(Long studyId) {
        return DataAccessTranslator.execute("listar los turnos del estudio " + studyId,
                () -> jdbc.queryForList("SELECT id, id_estudio, numero_dia, tipo, estado, id_cuadrilla FROM TURNO "
                                + "WHERE id_estudio = :estudio ORDER BY numero_dia, tipo",
                        new MapSqlParameterSource("estudio", studyId)).stream().map(this::toShift).toList());
    }

    @Override
    public void updateStatus(Long shiftId, ShiftStatus status) {
        DataAccessTranslator.execute("cambiar el estado del turno " + shiftId, () -> writeStatus(shiftId, status));
    }

    private void writeStatus(Long shiftId, ShiftStatus status) {
        jdbc.update("UPDATE TURNO SET estado = CAST(:estado AS estado_turno), "
                        + "cerrado_en = CASE WHEN :estado = 'CERRADO' THEN COALESCE(cerrado_en, now()) END WHERE id = :id",
                new MapSqlParameterSource("id", shiftId).addValue("estado", status.name()));
    }

    private void savePopulation(SamplingShift shift) {
        ShiftPopulation population = shift.getPopulation();
        if (population == null) {
            return;
        }
        MapSqlParameterSource params = FieldRecordMappers.addVerification(new MapSqlParameterSource()
                .addValue("turno", shift.getId())
                .addValue("estudiantes", population.getStudents())
                .addValue("docentes", population.getTeachers())
                .addValue("personal", population.getStaff()), population);
        if (population.getId() == null) {
            population.setId(jdbc.queryForObject("INSERT INTO POBLACION_TURNO (id_turno, estudiantes, docentes, personal, "
                    + "estado_verificacion, digitado_por, digitado_en, verificado_por, verificado_en) VALUES (:turno, "
                    + ":estudiantes, :docentes, :personal, CAST(:estado AS estado_verificacion), :digitadoPor, :digitadoEn, "
                    + ":verificadoPor, :verificadoEn) RETURNING id", params, Long.class));
        } else {
            jdbc.update("UPDATE POBLACION_TURNO SET estudiantes = :estudiantes, docentes = :docentes, personal = :personal, "
                    + "estado_verificacion = CAST(:estado AS estado_verificacion), verificado_por = :verificadoPor, "
                    + "verificado_en = :verificadoEn WHERE id = :id", params.addValue("id", population.getId()));
        }
    }

    private SamplingShift toShift(Map<String, Object> row) {
        Long id = ((Number) row.get("id")).longValue();
        MapSqlParameterSource byShift = new MapSqlParameterSource("turno", id);
        ShiftPopulation population = jdbc.query("SELECT id, estudiantes, docentes, personal, "
                        + FieldRecordMappers.VERIFICATION_COLUMNS + " FROM POBLACION_TURNO WHERE id_turno = :turno",
                byShift, FieldRecordMappers.POPULATION).stream().findFirst().orElse(null);
        LinkedHashSet<Long> withoutGeneration = new LinkedHashSet<>(jdbc.queryForList(
                "SELECT id_fuente FROM TURNO_SIN_GENERACION WHERE id_turno = :turno ORDER BY id_fuente", byShift, Long.class));
        SamplingShift shift = SamplingShift.restore(id, ((Number) row.get("id_estudio")).longValue(),
                ((Number) row.get("numero_dia")).intValue(), ShiftType.valueOf(row.get("tipo").toString()),
                ShiftStatus.valueOf(row.get("estado").toString()), population, bags.findByShift(id),
                quarterings.findByShift(id).orElse(null), densities.findByShift(id).orElse(null), withoutGeneration);
        Object crew = row.get("id_cuadrilla");
        shift.assignCrew(crew == null ? null : ((Number) crew).longValue());
        return shift;
    }
}
