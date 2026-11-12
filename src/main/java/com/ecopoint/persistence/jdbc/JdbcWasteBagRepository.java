package com.ecopoint.persistence.jdbc;

import com.ecopoint.model.WasteBag;
import com.ecopoint.persistence.WasteBagRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL implementation of {@link WasteBagRepository} (table {@code BOLSA}).
 * Net weight and density are generated columns, never written (R-014).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Repository
public class JdbcWasteBagRepository implements WasteBagRepository {

    private static final String SELECT = "SELECT " + FieldRecordMappers.BAG_COLUMNS
            + " FROM BOLSA b JOIN FUENTE_GENERACION f ON f.id = b.id_fuente ";

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Creates the repository.
     *
     * @param jdbc Spring JDBC template with named parameters
     */
    public JdbcWasteBagRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public WasteBag save(Long shiftId, WasteBag bag) {
        MapSqlParameterSource params = FieldRecordMappers.addVerification(new MapSqlParameterSource()
                .addValue("turno", shiftId)
                .addValue("fuente", bag.getSourceId())
                .addValue("rotulo", bag.getLabelText())
                .addValue("bruto", bag.getGrossWeight())
                .addValue("tara", bag.getTare())
                .addValue("volumen", bag.getVolume())
                .addValue("atipica", bag.isAtypical()), bag);
        if (bag.getId() == null) {
            bag.setId(DataAccessTranslator.execute("guardar la bolsa " + bag.getLabelText(),
                    () -> jdbc.queryForObject("INSERT INTO BOLSA (id_turno, id_fuente, rotulo, peso_bruto, tara, volumen, "
                            + "atipica, estado_verificacion, digitado_por, digitado_en, verificado_por, verificado_en) VALUES "
                            + "(:turno, :fuente, :rotulo, :bruto, :tara, :volumen, :atipica, "
                            + "CAST(:estado AS estado_verificacion), :digitadoPor, :digitadoEn, :verificadoPor, :verificadoEn) "
                            + "RETURNING id", params, Long.class)));
        } else {
            DataAccessTranslator.execute("actualizar la bolsa " + bag.getLabelText(),
                    () -> jdbc.update("UPDATE BOLSA SET peso_bruto = :bruto, tara = :tara, volumen = :volumen, "
                            + "atipica = :atipica, estado_verificacion = CAST(:estado AS estado_verificacion), "
                            + "verificado_por = :verificadoPor, verificado_en = :verificadoEn WHERE id = :id",
                            params.addValue("id", bag.getId())));
        }
        return bag;
    }

    @Override
    public Optional<WasteBag> findById(Long id) {
        return DataAccessTranslator.execute("buscar la bolsa " + id,
                () -> jdbc.query(SELECT + "WHERE b.id = :id", new MapSqlParameterSource("id", id),
                        FieldRecordMappers.BAG).stream().findFirst());
    }

    @Override
    public List<WasteBag> findByShift(Long shiftId) {
        return DataAccessTranslator.execute("listar las bolsas del turno " + shiftId,
                () -> jdbc.query(SELECT + "WHERE b.id_turno = :turno ORDER BY b.id",
                        new MapSqlParameterSource("turno", shiftId), FieldRecordMappers.BAG));
    }
}
