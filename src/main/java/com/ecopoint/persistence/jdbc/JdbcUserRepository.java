package com.ecopoint.persistence.jdbc;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.persistence.UserRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * PostgreSQL implementation of {@link UserRepository} (table {@code USUARIO}).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Repository
public class JdbcUserRepository implements UserRepository {

    /** Columns of a user, prefixed with the alias {@code u}. Shared with the crew repository. */
    static final String COLUMNS = "u.id, u.nombre, u.apellido, u.correo, u.rol, u.id_institucion, u.activo";

    /** Row mapper of a user. Shared with the crew repository. */
    static final RowMapper<User> MAPPER = (rs, row) -> {
        User user = new User(rs.getLong("id"), rs.getString("nombre"), rs.getString("apellido"),
                rs.getString("correo"), UserRole.valueOf(rs.getString("rol")), rs.getLong("id_institucion"));
        if (!rs.getBoolean("activo")) {
            user.deactivate();
        }
        return user;
    };

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcUserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public User save(User user) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("nombre", user.getFirstName().trim())
                .addValue("apellido", user.getLastName().trim())
                .addValue("correo", user.getEmail().trim())
                .addValue("rol", user.getRole().name())
                .addValue("institucion", user.getInstitutionId())
                .addValue("activo", user.isActive());
        if (user.getId() == null) {
            Long id = DataAccessTranslator.execute("guardar el usuario " + user.getEmail(),
                    () -> jdbc.queryForObject("INSERT INTO USUARIO (nombre, apellido, correo, rol, id_institucion, activo) "
                            + "VALUES (:nombre, :apellido, :correo, CAST(:rol AS rol_usuario), :institucion, :activo) "
                            + "RETURNING id", params, Long.class));
            user.setId(id);
        } else {
            DataAccessTranslator.execute("actualizar el usuario " + user.getEmail(),
                    () -> jdbc.update("UPDATE USUARIO SET nombre = :nombre, apellido = :apellido, correo = :correo, "
                            + "rol = CAST(:rol AS rol_usuario), id_institucion = :institucion, activo = :activo "
                            + "WHERE id = :id", params.addValue("id", user.getId())));
        }
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return DataAccessTranslator.execute("buscar el usuario " + id,
                () -> jdbc.query("SELECT " + COLUMNS + " FROM USUARIO u WHERE u.id = :id",
                        new MapSqlParameterSource("id", id), MAPPER).stream().findFirst());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return DataAccessTranslator.execute("buscar el usuario por correo",
                () -> jdbc.query("SELECT " + COLUMNS + " FROM USUARIO u WHERE lower(u.correo) = lower(btrim(:correo))",
                        new MapSqlParameterSource("correo", email), MAPPER).stream().findFirst());
    }

    @Override
    public List<User> findAll() {
        return DataAccessTranslator.execute("listar los usuarios",
                () -> jdbc.query("SELECT " + COLUMNS + " FROM USUARIO u ORDER BY u.apellido, u.nombre", MAPPER));
    }

    @Override
    public List<User> findByInstitution(Long institutionId) {
        return DataAccessTranslator.execute("listar los usuarios de la institución " + institutionId,
                () -> jdbc.query("SELECT " + COLUMNS + " FROM USUARIO u WHERE u.id_institucion = :institucion "
                        + "ORDER BY u.apellido, u.nombre", new MapSqlParameterSource("institucion", institutionId), MAPPER));
    }

    @Override
    public Set<String> findEmails() {
        return DataAccessTranslator.execute("consultar los correos registrados",
                () -> new LinkedHashSet<>(jdbc.getJdbcTemplate().queryForList(
                        "SELECT correo FROM USUARIO ORDER BY correo", String.class)));
    }

    @Override
    public void deactivate(Long userId) {
        DataAccessTranslator.execute("desactivar el usuario " + userId,
                () -> jdbc.update("UPDATE USUARIO SET activo = FALSE WHERE id = :id",
                        new MapSqlParameterSource("id", userId)));
    }

    @Override
    public Optional<String> findPasswordHashByEmail(String email) {
        return DataAccessTranslator.execute("consultar la credencial del usuario",
                () -> jdbc.queryForList("SELECT clave_hash FROM USUARIO WHERE lower(correo) = lower(btrim(:correo)) "
                        + "AND clave_hash IS NOT NULL", new MapSqlParameterSource("correo", email), String.class)
                        .stream().findFirst());
    }

    @Override
    public void updatePasswordHash(Long userId, String passwordHash) {
        int updated = DataAccessTranslator.execute("cambiar la contraseña del usuario " + userId,
                () -> jdbc.update("UPDATE USUARIO SET clave_hash = :hash WHERE id = :id",
                        new MapSqlParameterSource("hash", passwordHash).addValue("id", userId)));
        if (updated == 0) {
            throw new ResourceNotFoundException("Usuario", userId);
        }
    }
}
