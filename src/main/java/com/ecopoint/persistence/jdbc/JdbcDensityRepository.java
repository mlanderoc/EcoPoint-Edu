package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.DensityMeasurement;
import com.ecopoint.persistence.DensityRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PostgreSQL implementation of {@link DensityRepository} (table
 * {@code MEDICION_DENSIDAD}). The density is a generated column (R-014).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Repository
public class JdbcDensityRepository implements DensityRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcDensityRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DensityMeasurement save(DensityMeasurement measurement) {
        MapSqlParameterSource params = FieldRecordMappers.addVerification(new MapSqlParameterSource()
                .addValue("turno", measurement.getShiftId())
                .addValue("volumen", measurement.getContainerVolume())
                .addValue("peso", measurement.getNetWeight()), measurement);
        if (measurement.getId() == null) {
            measurement.setId(DataAccessTranslator.execute("guardar la densidad del turno " + measurement.getShiftId(),
                    () -> jdbc.queryForObject("INSERT INTO MEDICION_DENSIDAD (id_turno, volumen_recipiente, peso_neto, "
                            + "estado_verificacion, digitado_por, digitado_en, verificado_por, verificado_en) VALUES "
                            + "(:turno, :volumen, :peso, CAST(:estado AS estado_verificacion), :digitadoPor, :digitadoEn, "
                            + ":verificadoPor, :verificadoEn) RETURNING id", params, Long.class)));
        } else {
            DataAccessTranslator.execute("actualizar la densidad del turno " + measurement.getShiftId(),
                    () -> jdbc.update("UPDATE MEDICION_DENSIDAD SET volumen_recipiente = :volumen, peso_neto = :peso, "
                            + "estado_verificacion = CAST(:estado AS estado_verificacion), verificado_por = :verificadoPor, "
                            + "verificado_en = :verificadoEn WHERE id = :id", params.addValue("id", measurement.getId())));
        }
        return measurement;
    }

    @Override
    public Optional<DensityMeasurement> findByShift(Long shiftId) {
        return DataAccessTranslator.execute("buscar la densidad del turno " + shiftId,
                () -> jdbc.query("SELECT id, id_turno, volumen_recipiente, peso_neto, "
                                + FieldRecordMappers.VERIFICATION_COLUMNS + " FROM MEDICION_DENSIDAD WHERE id_turno = :turno",
                        new MapSqlParameterSource("turno", shiftId), FieldRecordMappers.DENSITY).stream().findFirst());
    }
}
