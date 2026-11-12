package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.BagLabel;
import com.ecopoint.model.DensityMeasurement;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.VerifiableRecord;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.VerificationStatus;
import com.ecopoint.model.WasteBag;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Row mappers and parameters shared by the repositories of field records.
 * The four verifiable tables have the same verification columns (R-030):
 * {@code estado_verificacion, digitado_por, digitado_en, verificado_por, verificado_en}.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
final class FieldRecordMappers {

    /** Verification columns, to append to a SELECT. */
    static final String VERIFICATION_COLUMNS = "estado_verificacion, digitado_por, digitado_en, verificado_por, verificado_en";

    /** Columns of a bag, including the code of its source (alias {@code f}). */
    static final String BAG_COLUMNS = "b.id, b.id_turno, b.id_fuente, f.codigo AS codigo_fuente, b.rotulo, b.peso_bruto, "
            + "b.tara, b.volumen, b.atipica, b.estado_verificacion, b.digitado_por, b.digitado_en, b.verificado_por, "
            + "b.verificado_en";

    /** Population of a shift. */
    static final RowMapper<ShiftPopulation> POPULATION = (rs, row) -> new ShiftPopulation(rs.getLong("id"),
            rs.getInt("estudiantes"), rs.getInt("docentes"), rs.getInt("personal"), verification(rs));

    /** Bag, read with {@link #BAG_COLUMNS}. */
    static final RowMapper<WasteBag> BAG = (rs, row) -> WasteBag.restore(rs.getLong("id"), rs.getLong("id_fuente"),
            rs.getString("codigo_fuente"), BagLabel.parse(rs.getString("rotulo")), rs.getBigDecimal("peso_bruto"),
            rs.getBigDecimal("tara"), rs.getBigDecimal("volumen"), rs.getBoolean("atipica"), verification(rs));

    /** Density measurement. */
    static final RowMapper<DensityMeasurement> DENSITY = (rs, row) -> new DensityMeasurement(rs.getLong("id"),
            rs.getLong("id_turno"), rs.getBigDecimal("volumen_recipiente"), rs.getBigDecimal("peso_neto"),
            verification(rs));

    private FieldRecordMappers() {
    }

    /**
     * Reads the verification columns of the current row.
     *
     * @param rs result set positioned on a row
     * @return the verification data
     * @throws SQLException if a column cannot be read
     */
    static VerificationInfo verification(ResultSet rs) throws SQLException {
        long verifiedBy = rs.getLong("verificado_por");
        Long verifier = rs.wasNull() ? null : verifiedBy;
        return VerificationInfo.restore(VerificationStatus.valueOf(rs.getString("estado_verificacion")),
                rs.getLong("digitado_por"), toLocal(rs.getTimestamp("digitado_en")),
                verifier, toLocal(rs.getTimestamp("verificado_en")));
    }

    /**
     * Adds the verification values of a record to the parameters, with the names
     * {@code estado, digitadoPor, digitadoEn, verificadoPor, verificadoEn}.
     *
     * @param params parameters to complete
     * @param record verifiable record
     * @return the same parameters
     */
    static MapSqlParameterSource addVerification(MapSqlParameterSource params, VerifiableRecord record) {
        VerificationInfo info = record.getVerification();
        return params.addValue("estado", info.getStatus().name())
                .addValue("digitadoPor", info.getEnteredBy())
                .addValue("digitadoEn", toTimestamp(info.getEnteredAt()))
                .addValue("verificadoPor", info.getVerifiedBy())
                .addValue("verificadoEn", toTimestamp(info.getVerifiedAt()));
    }

    /**
     * Converts a date-time to a JDBC timestamp.
     *
     * @param value date-time, or null
     * @return timestamp, or null
     */
    static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    /**
     * Converts a JDBC timestamp to a date-time.
     *
     * @param value timestamp, or null
     * @return date-time, or null
     */
    static LocalDateTime toLocal(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
