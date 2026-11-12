package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.SourceType;
import com.ecopoint.persistence.GenerationSourceRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * PostgreSQL implementation of {@link GenerationSourceRepository} (table
 * {@code FUENTE_GENERACION}). The polygon is stored as GeoJSON in a
 * {@code jsonb} column (R-032).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Repository
public class JdbcGenerationSourceRepository implements GenerationSourceRepository {

    private static final String COLUMNS = "id, id_institucion, codigo, nombre, tipo, geometria::text AS geometria, activa";

    private static final RowMapper<GenerationSource> MAPPER = (rs, row) -> {
        GenerationSource source = new GenerationSource(rs.getLong("id"), rs.getLong("id_institucion"),
                rs.getString("codigo"), rs.getString("nombre"), SourceType.valueOf(rs.getString("tipo")));
        source.setGeometry(GeoJsonMapper.fromGeoJson(rs.getString("geometria")));
        if (!rs.getBoolean("activa")) {
            source.deactivate();
        }
        return source;
    };

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcGenerationSourceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public GenerationSource save(GenerationSource source) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("institucion", source.getInstitutionId())
                .addValue("codigo", source.getCode())
                .addValue("nombre", source.getName())
                .addValue("tipo", source.getType().name())
                .addValue("geometria", GeoJsonMapper.toGeoJson(source.getGeometry()))
                .addValue("activa", source.isActive());
        if (source.getId() == null) {
            Long id = DataAccessTranslator.execute("guardar la fuente " + source.getCode(),
                    () -> jdbc.queryForObject("INSERT INTO FUENTE_GENERACION (id_institucion, codigo, nombre, tipo, "
                            + "geometria, activa) VALUES (:institucion, :codigo, :nombre, CAST(:tipo AS tipo_fuente), "
                            + "CAST(:geometria AS jsonb), :activa) RETURNING id", params, Long.class));
            source.setId(id);
        } else {
            DataAccessTranslator.execute("actualizar la fuente " + source.getCode(),
                    () -> jdbc.update("UPDATE FUENTE_GENERACION SET id_institucion = :institucion, codigo = :codigo, "
                            + "nombre = :nombre, tipo = CAST(:tipo AS tipo_fuente), geometria = CAST(:geometria AS jsonb), "
                            + "activa = :activa WHERE id = :id", params.addValue("id", source.getId())));
        }
        return source;
    }

    @Override
    public Optional<GenerationSource> findById(Long id) {
        return DataAccessTranslator.execute("buscar la fuente " + id,
                () -> jdbc.query("SELECT " + COLUMNS + " FROM FUENTE_GENERACION WHERE id = :id",
                        new MapSqlParameterSource("id", id), MAPPER).stream().findFirst());
    }

    @Override
    public List<GenerationSource> findByInstitution(Long institutionId) {
        return DataAccessTranslator.execute("listar las fuentes de la institución " + institutionId,
                () -> jdbc.query("SELECT " + COLUMNS + " FROM FUENTE_GENERACION WHERE id_institucion = :institucion "
                        + "ORDER BY codigo", new MapSqlParameterSource("institucion", institutionId), MAPPER));
    }

    @Override
    public Set<String> findCodesByInstitution(Long institutionId) {
        return DataAccessTranslator.execute("consultar los códigos de fuente de la institución " + institutionId,
                () -> new LinkedHashSet<>(jdbc.queryForList("SELECT codigo FROM FUENTE_GENERACION "
                        + "WHERE id_institucion = :institucion ORDER BY codigo",
                        new MapSqlParameterSource("institucion", institutionId), String.class)));
    }

    @Override
    public boolean hasBags(Long sourceId) {
        return DataAccessTranslator.execute("consultar si la fuente " + sourceId + " tiene bolsas",
                () -> Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM BOLSA WHERE id_fuente = :id)",
                        new MapSqlParameterSource("id", sourceId), Boolean.class)));
    }

    @Override
    public void deactivate(Long sourceId) {
        DataAccessTranslator.execute("desactivar la fuente " + sourceId,
                () -> jdbc.update("UPDATE FUENTE_GENERACION SET activa = FALSE WHERE id = :id",
                        new MapSqlParameterSource("id", sourceId)));
    }

    @Override
    public void delete(Long sourceId) {
        DataAccessTranslator.execute("eliminar la fuente " + sourceId,
                () -> jdbc.update("DELETE FROM FUENTE_GENERACION WHERE id = :id",
                        new MapSqlParameterSource("id", sourceId)));
    }
}
