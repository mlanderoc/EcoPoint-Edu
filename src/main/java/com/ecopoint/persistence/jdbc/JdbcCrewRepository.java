package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.Crew;
import com.ecopoint.model.User;
import com.ecopoint.persistence.CrewRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PostgreSQL implementation of {@link CrewRepository} (tables {@code CUADRILLA}
 * and {@code CUADRILLA_INTEGRANTE}). A crew and its members are saved in one
 * transaction (R-020).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Repository
public class JdbcCrewRepository implements CrewRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcCrewRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Crew save(Crew crew) {
        DataAccessTranslator.execute("guardar la cuadrilla " + crew.getName(), () -> {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("nombre", crew.getName().trim())
                    .addValue("responsable", crew.getResponsible().getId());
            if (crew.getId() == null) {
                crew.setId(jdbc.queryForObject("INSERT INTO CUADRILLA (nombre, id_responsable) "
                        + "VALUES (:nombre, :responsable) RETURNING id", params, Long.class));
            } else {
                jdbc.update("UPDATE CUADRILLA SET nombre = :nombre, id_responsable = :responsable WHERE id = :id",
                        params.addValue("id", crew.getId()));
                jdbc.update("DELETE FROM CUADRILLA_INTEGRANTE WHERE id_cuadrilla = :id",
                        new MapSqlParameterSource("id", crew.getId()));
            }
            for (User member : crew.getMembers()) {
                jdbc.update("INSERT INTO CUADRILLA_INTEGRANTE (id_cuadrilla, id_usuario) VALUES (:cuadrilla, :usuario)",
                        new MapSqlParameterSource("cuadrilla", crew.getId()).addValue("usuario", member.getId()));
            }
        });
        return crew;
    }

    @Override
    public Optional<Crew> findById(Long id) {
        return DataAccessTranslator.execute("buscar la cuadrilla " + id,
                () -> jdbc.queryForList("SELECT id, nombre, id_responsable FROM CUADRILLA WHERE id = :id",
                        new MapSqlParameterSource("id", id)).stream().findFirst().map(this::toCrew));
    }

    @Override
    public List<Crew> findByInstitution(Long institutionId) {
        return DataAccessTranslator.execute("listar las cuadrillas de la institución " + institutionId,
                () -> jdbc.queryForList("SELECT c.id, c.nombre, c.id_responsable FROM CUADRILLA c "
                        + "JOIN USUARIO r ON r.id = c.id_responsable WHERE r.id_institucion = :institucion "
                        + "ORDER BY c.nombre", new MapSqlParameterSource("institucion", institutionId))
                        .stream().map(this::toCrew).toList());
    }

    private Crew toCrew(Map<String, Object> row) {
        Long crewId = ((Number) row.get("id")).longValue();
        User responsible = jdbc.queryForObject("SELECT " + JdbcUserRepository.COLUMNS + " FROM USUARIO u WHERE u.id = :id",
                new MapSqlParameterSource("id", row.get("id_responsable")), JdbcUserRepository.MAPPER);
        Crew crew = new Crew(crewId, (String) row.get("nombre"), responsible);
        jdbc.query("SELECT " + JdbcUserRepository.COLUMNS + " FROM CUADRILLA_INTEGRANTE ci "
                        + "JOIN USUARIO u ON u.id = ci.id_usuario WHERE ci.id_cuadrilla = :id ORDER BY u.apellido, u.nombre",
                new MapSqlParameterSource("id", crewId), JdbcUserRepository.MAPPER).forEach(crew::addMember);
        return crew;
    }
}
