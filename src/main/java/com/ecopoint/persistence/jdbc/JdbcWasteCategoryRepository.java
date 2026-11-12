package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import com.ecopoint.persistence.WasteCategoryRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * PostgreSQL implementation of {@link WasteCategoryRepository} (table
 * {@code SUBCATEGORIA_RESIDUO}).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Repository
public class JdbcWasteCategoryRepository implements WasteCategoryRepository {

    private static final String COLUMNS = "id, nombre, grupo, descripcion, activa";

    private static final RowMapper<WasteCategory> MAPPER = (rs, row) -> {
        WasteCategory category = new WasteCategory(rs.getLong("id"), rs.getString("nombre"),
                WasteGroup.valueOf(rs.getString("grupo")), rs.getString("descripcion"));
        if (!rs.getBoolean("activa")) {
            category.deactivate();
        }
        return category;
    };

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcWasteCategoryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public WasteCategory save(WasteCategory category) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("nombre", category.getName().trim())
                .addValue("grupo", category.getGroup().name())
                .addValue("descripcion", category.getDescription())
                .addValue("activa", category.isActive());
        if (category.getId() == null) {
            Long id = DataAccessTranslator.execute("guardar la subcategoría " + category.getName(),
                    () -> jdbc.queryForObject("INSERT INTO SUBCATEGORIA_RESIDUO (nombre, grupo, descripcion, activa) "
                            + "VALUES (:nombre, CAST(:grupo AS grupo_residuo), :descripcion, :activa) RETURNING id",
                            params, Long.class));
            category.setId(id);
        } else {
            DataAccessTranslator.execute("actualizar la subcategoría " + category.getName(),
                    () -> jdbc.update("UPDATE SUBCATEGORIA_RESIDUO SET nombre = :nombre, "
                            + "grupo = CAST(:grupo AS grupo_residuo), descripcion = :descripcion, activa = :activa "
                            + "WHERE id = :id", params.addValue("id", category.getId())));
        }
        return category;
    }

    @Override
    public Optional<WasteCategory> findById(Long id) {
        return DataAccessTranslator.execute("buscar la subcategoría " + id,
                () -> jdbc.query("SELECT " + COLUMNS + " FROM SUBCATEGORIA_RESIDUO WHERE id = :id",
                        new MapSqlParameterSource("id", id), MAPPER).stream().findFirst());
    }

    @Override
    public List<WasteCategory> findAll() {
        return DataAccessTranslator.execute("listar las subcategorías de residuos",
                () -> jdbc.query("SELECT " + COLUMNS + " FROM SUBCATEGORIA_RESIDUO ORDER BY grupo, nombre", MAPPER));
    }

    @Override
    public Map<Long, WasteCategory> findAllAsMap() {
        Map<Long, WasteCategory> catalog = new LinkedHashMap<>();
        findAll().forEach(category -> catalog.put(category.getId(), category));
        return catalog;
    }

    @Override
    public Set<String> findNames() {
        return DataAccessTranslator.execute("consultar los nombres de subcategoría",
                () -> new LinkedHashSet<>(jdbc.getJdbcTemplate().queryForList(
                        "SELECT nombre FROM SUBCATEGORIA_RESIDUO ORDER BY nombre", String.class)));
    }

    @Override
    public boolean hasResults(Long categoryId) {
        return DataAccessTranslator.execute("consultar si la subcategoría " + categoryId + " tiene resultados",
                () -> Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM RESULTADO_CUARTEO WHERE id_subcategoria = :id)",
                        new MapSqlParameterSource("id", categoryId), Boolean.class)));
    }
}
