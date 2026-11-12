package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.Correction;
import com.ecopoint.persistence.CorrectionRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PostgreSQL implementation of {@link CorrectionRepository} (table
 * {@code CORRECCION}). A trigger in the database blocks any update or delete
 * of the history (R-031, CA-107).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Repository
public class JdbcCorrectionRepository implements CorrectionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcCorrectionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Long save(Correction correction) {
        return DataAccessTranslator.execute("guardar la corrección de " + correction.recordType().toLowerCase(),
                () -> jdbc.queryForObject("INSERT INTO CORRECCION (tipo_registro, id_registro, campo, valor_anterior, "
                                + "valor_nuevo, motivo, id_usuario, corregido_en) VALUES (:tipo, :registro, :campo, "
                                + ":anterior, :nuevo, :motivo, :usuario, :fecha) RETURNING id",
                        new MapSqlParameterSource("tipo", correction.recordType())
                                .addValue("registro", correction.recordId())
                                .addValue("campo", correction.field())
                                .addValue("anterior", correction.oldValue())
                                .addValue("nuevo", correction.newValue())
                                .addValue("motivo", correction.reason())
                                .addValue("usuario", correction.userId())
                                .addValue("fecha", FieldRecordMappers.toTimestamp(correction.at())), Long.class));
    }

    @Override
    public List<Correction> findByRecord(String recordType, Long recordId) {
        return DataAccessTranslator.execute("consultar el historial de correcciones",
                () -> jdbc.query("SELECT tipo_registro, id_registro, campo, valor_anterior, valor_nuevo, motivo, "
                                + "id_usuario, corregido_en FROM CORRECCION WHERE tipo_registro = :tipo "
                                + "AND id_registro = :registro ORDER BY id",
                        new MapSqlParameterSource("tipo", recordType).addValue("registro", recordId),
                        (rs, row) -> new Correction(rs.getString("tipo_registro"), rs.getLong("id_registro"),
                                rs.getString("campo"), rs.getString("valor_anterior"), rs.getString("valor_nuevo"),
                                rs.getString("motivo"), rs.getLong("id_usuario"),
                                FieldRecordMappers.toLocal(rs.getTimestamp("corregido_en")))));
    }
}
