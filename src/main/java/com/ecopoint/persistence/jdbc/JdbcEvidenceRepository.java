package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.Evidence;
import com.ecopoint.model.EvidenceTarget;
import com.ecopoint.persistence.EvidenceRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL implementation of {@link EvidenceRepository} (table {@code EVIDENCIA}).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Repository
public class JdbcEvidenceRepository implements EvidenceRepository {

    private static final String SELECT = "SELECT id, tipo, id_registro, ruta_archivo, tipo_medio, tamano_bytes, "
            + "descripcion, subida_por, subida_en FROM EVIDENCIA ";
    private static final RowMapper<Evidence> MAPPER = (rs, row) -> new Evidence(rs.getLong("id"),
            EvidenceTarget.valueOf(rs.getString("tipo")), rs.getLong("id_registro"), rs.getString("ruta_archivo"),
            rs.getString("tipo_medio"), rs.getLong("tamano_bytes"), rs.getString("descripcion"),
            rs.getLong("subida_por"), FieldRecordMappers.toLocal(rs.getTimestamp("subida_en")));

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcEvidenceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Evidence save(Evidence evidence) {
        evidence.setId(DataAccessTranslator.execute("guardar la evidencia " + evidence.getFilePath(),
                () -> jdbc.queryForObject("INSERT INTO EVIDENCIA (tipo, id_registro, ruta_archivo, tipo_medio, tamano_bytes, "
                                + "descripcion, subida_por, subida_en) VALUES (CAST(:tipo AS tipo_evidencia), :registro, :ruta, "
                                + ":medio, :tamano, :descripcion, :usuario, :fecha) RETURNING id",
                        new MapSqlParameterSource("tipo", evidence.getTarget().name())
                                .addValue("registro", evidence.getTargetId())
                                .addValue("ruta", evidence.getFilePath())
                                .addValue("medio", evidence.getMimeType())
                                .addValue("tamano", evidence.getSizeBytes())
                                .addValue("descripcion", evidence.getDescription())
                                .addValue("usuario", evidence.getUploadedBy())
                                .addValue("fecha", FieldRecordMappers.toTimestamp(evidence.getUploadedAt())), Long.class)));
        return evidence;
    }

    @Override
    public List<Evidence> findByTarget(EvidenceTarget target, Long targetId) {
        return DataAccessTranslator.execute("listar las evidencias del registro " + targetId,
                () -> jdbc.query(SELECT + "WHERE tipo = CAST(:tipo AS tipo_evidencia) AND id_registro = :registro ORDER BY id",
                        new MapSqlParameterSource("tipo", target.name()).addValue("registro", targetId), MAPPER));
    }

    @Override
    public Optional<Evidence> findById(Long id) {
        return DataAccessTranslator.execute("buscar la evidencia " + id,
                () -> jdbc.query(SELECT + "WHERE id = :id", new MapSqlParameterSource("id", id), MAPPER)
                        .stream().findFirst());
    }
}
