package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.QualityStatus;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.WasteGroup;
import com.ecopoint.persistence.QuarteringRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL implementation of {@link QuarteringRepository} (tables
 * {@code CUARTEO} and {@code RESULTADO_CUARTEO}). The quartering and its
 * results are written in one transaction: if one result fails, nothing is
 * stored (R-020, CA-105).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Repository
public class JdbcQuarteringRepository implements QuarteringRepository {

    private static final String SELECT = "SELECT id, id_turno, peso_neto_turno, peso_submuestra, estado_calidad, "
            + "motivo_anulacion, anulado_por, anulado_en, " + FieldRecordMappers.VERIFICATION_COLUMNS + " FROM CUARTEO ";

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcQuarteringRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Quartering save(Quartering quartering) {
        DataAccessTranslator.execute("guardar el cuarteo del turno " + quartering.getShiftId(), () -> {
            MapSqlParameterSource params = FieldRecordMappers.addVerification(new MapSqlParameterSource()
                    .addValue("turno", quartering.getShiftId())
                    .addValue("netoTurno", quartering.getShiftNetWeight())
                    .addValue("submuestra", quartering.getSampleWeight())
                    .addValue("calidad", quartering.getQualityStatus().name())
                    .addValue("motivo", quartering.getAnnulReason())
                    .addValue("anuladoPor", quartering.getAnnulledBy())
                    .addValue("anuladoEn", FieldRecordMappers.toTimestamp(quartering.getAnnulledAt())), quartering);
            if (quartering.getId() == null) {
                quartering.setId(jdbc.queryForObject("INSERT INTO CUARTEO (id_turno, peso_neto_turno, peso_submuestra, "
                        + "estado_calidad, motivo_anulacion, anulado_por, anulado_en, estado_verificacion, digitado_por, "
                        + "digitado_en, verificado_por, verificado_en) VALUES (:turno, :netoTurno, :submuestra, "
                        + "CAST(:calidad AS estado_calidad), :motivo, :anuladoPor, :anuladoEn, "
                        + "CAST(:estado AS estado_verificacion), :digitadoPor, :digitadoEn, :verificadoPor, :verificadoEn) "
                        + "RETURNING id", params, Long.class));
            } else {
                jdbc.update("UPDATE CUARTEO SET peso_submuestra = :submuestra, estado_calidad = CAST(:calidad AS estado_calidad), "
                        + "motivo_anulacion = :motivo, anulado_por = :anuladoPor, anulado_en = :anuladoEn, "
                        + "estado_verificacion = CAST(:estado AS estado_verificacion), verificado_por = :verificadoPor, "
                        + "verificado_en = :verificadoEn WHERE id = :id", params.addValue("id", quartering.getId()));
                jdbc.update("DELETE FROM RESULTADO_CUARTEO WHERE id_cuarteo = :id",
                        new MapSqlParameterSource("id", quartering.getId()));
            }
            for (QuarteringResult result : quartering.getResults()) {
                jdbc.update("INSERT INTO RESULTADO_CUARTEO (id_cuarteo, id_subcategoria, peso) VALUES (:cuarteo, :sub, :peso)",
                        new MapSqlParameterSource("cuarteo", quartering.getId())
                                .addValue("sub", result.getCategoryId())
                                .addValue("peso", result.getWeight()));
            }
        });
        return quartering;
    }

    @Override
    public Optional<Quartering> findById(Long id) {
        return find("buscar el cuarteo " + id, "WHERE id = :value", id);
    }

    @Override
    public Optional<Quartering> findByShift(Long shiftId) {
        return find("buscar el cuarteo del turno " + shiftId, "WHERE id_turno = :value", shiftId);
    }

    private Optional<Quartering> find(String operation, String where, Long value) {
        return DataAccessTranslator.execute(operation, () -> jdbc.query(SELECT + where,
                new MapSqlParameterSource("value", value), (rs, row) -> {
                    long cuarteo = rs.getLong("id");
                    long annulledBy = rs.getLong("anulado_por");
                    Long annuller = rs.wasNull() ? null : annulledBy;
                    return Quartering.restore(cuarteo, rs.getLong("id_turno"), rs.getBigDecimal("peso_neto_turno"),
                            rs.getBigDecimal("peso_submuestra"), FieldRecordMappers.verification(rs), results(cuarteo),
                            QualityStatus.valueOf(rs.getString("estado_calidad")), rs.getString("motivo_anulacion"),
                            annuller, FieldRecordMappers.toLocal(rs.getTimestamp("anulado_en")));
                }).stream().findFirst());
    }

    private List<QuarteringResult> results(long quarteringId) {
        return jdbc.query("SELECT r.id_subcategoria, s.nombre, s.grupo, r.peso FROM RESULTADO_CUARTEO r "
                        + "JOIN SUBCATEGORIA_RESIDUO s ON s.id = r.id_subcategoria WHERE r.id_cuarteo = :id ORDER BY s.id",
                new MapSqlParameterSource("id", quarteringId),
                (rs, row) -> new QuarteringResult(rs.getLong("id_subcategoria"), rs.getString("nombre"),
                        WasteGroup.valueOf(rs.getString("grupo")), rs.getBigDecimal("peso")));
    }
}
