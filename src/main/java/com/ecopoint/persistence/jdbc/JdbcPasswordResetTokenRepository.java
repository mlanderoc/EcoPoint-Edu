package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.PasswordResetToken;
import com.ecopoint.persistence.PasswordResetTokenRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * PostgreSQL implementation of {@link PasswordResetTokenRepository} (table
 * {@code TOKEN_RECUPERACION}).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Repository
public class JdbcPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private static final RowMapper<PasswordResetToken> MAPPER = (rs, row) -> {
        Timestamp usedAt = rs.getTimestamp("usado_en");
        return new PasswordResetToken(rs.getLong("id"), rs.getLong("id_usuario"), rs.getString("codigo_hash"),
                rs.getTimestamp("creado_en").toLocalDateTime(), rs.getTimestamp("vence_en").toLocalDateTime(),
                usedAt == null ? null : usedAt.toLocalDateTime());
    };

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcPasswordResetTokenRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("usuario", token.getUserId())
                .addValue("hash", token.getCodeHash())
                .addValue("creado", token.getCreatedAt())
                .addValue("vence", token.getExpiresAt())
                .addValue("usado", token.getUsedAt());
        Long id = DataAccessTranslator.execute("guardar el código de recuperación",
                () -> jdbc.queryForObject("INSERT INTO TOKEN_RECUPERACION "
                        + "(id_usuario, codigo_hash, creado_en, vence_en, usado_en) "
                        + "VALUES (:usuario, :hash, :creado, :vence, :usado) RETURNING id", params, Long.class));
        token.setId(id);
        return token;
    }

    @Override
    public Optional<PasswordResetToken> findByHash(String codeHash) {
        return DataAccessTranslator.execute("buscar el código de recuperación",
                () -> jdbc.query("SELECT id, id_usuario, codigo_hash, creado_en, vence_en, usado_en "
                                + "FROM TOKEN_RECUPERACION WHERE codigo_hash = :hash",
                        new MapSqlParameterSource("hash", codeHash), MAPPER).stream().findFirst());
    }

    @Override
    public int countCreatedSince(Long userId, LocalDateTime since) {
        Integer count = DataAccessTranslator.execute("contar los códigos de recuperación",
                () -> jdbc.queryForObject("SELECT COUNT(*) FROM TOKEN_RECUPERACION "
                                + "WHERE id_usuario = :usuario AND creado_en > :desde",
                        new MapSqlParameterSource("usuario", userId).addValue("desde", since), Integer.class));
        return count == null ? 0 : count;
    }

    @Override
    public boolean markUsed(Long tokenId, LocalDateTime at) {
        int updated = DataAccessTranslator.execute("usar el código de recuperación",
                () -> jdbc.update("UPDATE TOKEN_RECUPERACION SET usado_en = :usado "
                                + "WHERE id = :id AND usado_en IS NULL",
                        new MapSqlParameterSource("usado", at).addValue("id", tokenId)));
        return updated == 1;
    }

    @Override
    public void invalidateAll(Long userId, LocalDateTime at) {
        DataAccessTranslator.execute("anular los códigos de recuperación",
                () -> jdbc.update("UPDATE TOKEN_RECUPERACION SET usado_en = :usado "
                                + "WHERE id_usuario = :usuario AND usado_en IS NULL",
                        new MapSqlParameterSource("usado", at).addValue("usuario", userId)));
    }
}
