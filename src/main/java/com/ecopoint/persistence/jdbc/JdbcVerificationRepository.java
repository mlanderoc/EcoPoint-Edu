package com.ecopoint.persistence.jdbc;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.DensityMeasurement;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.VerifiableRecord;
import com.ecopoint.model.WasteBag;
import com.ecopoint.persistence.PendingRecord;
import com.ecopoint.persistence.VerificationRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PostgreSQL implementation of {@link VerificationRepository}. It writes the
 * verification columns of the table that matches each kind of record and reads
 * the view {@code V_REGISTROS_PENDIENTES}.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Repository
public class JdbcVerificationRepository implements VerificationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcVerificationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void updateVerification(VerifiableRecord record) {
        String table = tableOf(record);
        MapSqlParameterSource params = FieldRecordMappers.addVerification(
                new MapSqlParameterSource("id", record.getId()), record);
        DataAccessTranslator.execute("guardar la verificación de " + record.getRecordType().toLowerCase() + " " + record.getId(),
                () -> jdbc.update("UPDATE " + table + " SET estado_verificacion = CAST(:estado AS estado_verificacion), "
                        + "verificado_por = :verificadoPor, verificado_en = :verificadoEn WHERE id = :id", params));
    }

    @Override
    public List<PendingRecord> findPendingByStudy(Long studyId) {
        return DataAccessTranslator.execute("listar los registros pendientes del estudio " + studyId,
                () -> jdbc.query("SELECT tipo_registro, id_registro, id_turno FROM V_REGISTROS_PENDIENTES "
                                + "WHERE id_estudio = :estudio ORDER BY id_turno, tipo_registro, id_registro",
                        new MapSqlParameterSource("estudio", studyId),
                        (rs, row) -> new PendingRecord(rs.getString("tipo_registro"), rs.getLong("id_registro"),
                                rs.getLong("id_turno"))));
    }

    private static String tableOf(VerifiableRecord record) {
        if (record instanceof ShiftPopulation) {
            return "POBLACION_TURNO";
        }
        if (record instanceof WasteBag) {
            return "BOLSA";
        }
        if (record instanceof Quartering) {
            return "CUARTEO";
        }
        if (record instanceof DensityMeasurement) {
            return "MEDICION_DENSIDAD";
        }
        throw new InvalidDataException("registro", "Tipo de registro verificable desconocido: " + record.getRecordType());
    }
}
