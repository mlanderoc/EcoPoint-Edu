package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.GeoPoint;
import com.ecopoint.model.Institution;
import com.ecopoint.persistence.InstitutionRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL implementation of {@link InstitutionRepository} with
 * {@code NamedParameterJdbcTemplate} (table {@code INSTITUCION}).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Repository
public class JdbcInstitutionRepository implements InstitutionRepository {

    private static final String COLUMNS = "id, nombre, direccion, activa, centro_latitud, centro_longitud, zoom_mapa, "
            + "peso_min_submuestra, peso_max_submuestra, tolerancia_calidad, peso_max_bolsa, "
            + "densidad_min_bolsa, densidad_max_bolsa";

    private static final RowMapper<Institution> MAPPER = (rs, row) -> {
        Institution institution = new Institution(rs.getLong("id"), rs.getString("nombre"), rs.getString("direccion"));
        BigDecimal latitude = rs.getBigDecimal("centro_latitud");
        if (latitude != null) {
            institution.setMapCenter(new GeoPoint(latitude, rs.getBigDecimal("centro_longitud")));
        }
        institution.setMapZoom(rs.getInt("zoom_mapa"));
        if (!rs.getBoolean("activa")) {
            institution.deactivate();
        }
        institution.setSettings(new CharacterizationSettings(
                rs.getBigDecimal("peso_min_submuestra"), rs.getBigDecimal("peso_max_submuestra"),
                rs.getBigDecimal("tolerancia_calidad"), rs.getBigDecimal("peso_max_bolsa"),
                rs.getBigDecimal("densidad_min_bolsa"), rs.getBigDecimal("densidad_max_bolsa")));
        return institution;
    };

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcInstitutionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Institution save(Institution institution) {
        MapSqlParameterSource params = parameters(institution);
        if (institution.getId() == null) {
            Long id = DataAccessTranslator.execute("guardar la institución " + institution.getName(),
                    () -> jdbc.queryForObject("INSERT INTO INSTITUCION (nombre, direccion, activa, centro_latitud, "
                            + "centro_longitud, zoom_mapa, peso_min_submuestra, peso_max_submuestra, "
                            + "tolerancia_calidad, peso_max_bolsa, densidad_min_bolsa, densidad_max_bolsa) "
                            + "VALUES (:nombre, :direccion, :activa, :lat, :lon, :zoom, :minSample, :maxSample, :tolerance, "
                            + ":maxBag, :minDensity, :maxDensity) RETURNING id", params, Long.class));
            institution.setId(id);
        } else {
            DataAccessTranslator.execute("actualizar la institución " + institution.getName(),
                    () -> jdbc.update("UPDATE INSTITUCION SET nombre = :nombre, direccion = :direccion, activa = :activa, "
                            + "centro_latitud = :lat, centro_longitud = :lon, zoom_mapa = :zoom, "
                            + "peso_min_submuestra = :minSample, peso_max_submuestra = :maxSample, "
                            + "tolerancia_calidad = :tolerance, peso_max_bolsa = :maxBag, "
                            + "densidad_min_bolsa = :minDensity, densidad_max_bolsa = :maxDensity WHERE id = :id",
                            params.addValue("id", institution.getId())));
        }
        return institution;
    }

    @Override
    public Optional<Institution> findById(Long id) {
        return DataAccessTranslator.execute("buscar la institución " + id,
                () -> jdbc.query("SELECT " + COLUMNS + " FROM INSTITUCION WHERE id = :id",
                        new MapSqlParameterSource("id", id), MAPPER).stream().findFirst());
    }

    @Override
    public List<Institution> findAll() {
        return DataAccessTranslator.execute("listar las instituciones",
                () -> jdbc.query("SELECT " + COLUMNS + " FROM INSTITUCION ORDER BY nombre", MAPPER));
    }

    @Override
    public boolean existsByName(String name) {
        return DataAccessTranslator.execute("buscar la institución " + name,
                () -> Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT EXISTS (SELECT 1 FROM INSTITUCION WHERE lower(nombre) = lower(btrim(:nombre)))",
                        new MapSqlParameterSource("nombre", name), Boolean.class)));
    }

    private static MapSqlParameterSource parameters(Institution institution) {
        CharacterizationSettings settings = institution.getSettings();
        GeoPoint center = institution.getMapCenter();
        return new MapSqlParameterSource()
                .addValue("nombre", institution.getName())
                .addValue("direccion", institution.getAddress())
                .addValue("activa", institution.isActive())
                .addValue("lat", center == null ? null : center.latitude())
                .addValue("lon", center == null ? null : center.longitude())
                .addValue("zoom", institution.getMapZoom())
                .addValue("minSample", settings.minSampleWeight())
                .addValue("maxSample", settings.maxSampleWeight())
                .addValue("tolerance", settings.qualityTolerancePercent())
                .addValue("maxBag", settings.maxBagNetWeight())
                .addValue("minDensity", settings.minBagDensity())
                .addValue("maxDensity", settings.maxBagDensity());
    }
}
