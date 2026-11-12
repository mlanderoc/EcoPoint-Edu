package com.ecopoint.persistence.jdbc;

import com.ecopoint.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;

import java.util.function.Supplier;

/**
 * Runs a database operation and translates any Spring {@link DataAccessException}
 * into a {@link PersistenceException} that names the operation in Spanish
 * (spec 002, R-021, decision T10). The original exception is kept as the cause
 * for the server log, but the message never shows SQL details to the user.
 *
 * @author mlanderoc
 * @version 1.0
 */
public final class DataAccessTranslator {

    private DataAccessTranslator() {
    }

    /**
     * Runs an operation that returns a value.
     *
     * @param operation description of the operation in Spanish, e.g. "guardar la fuente AU1"
     * @param action    database work to run
     * @param <T>       type of the result
     * @return the result of the action
     * @throws PersistenceException if the database reports an error
     */
    public static <T> T execute(String operation, Supplier<T> action) {
        try {
            return action.get();
        } catch (DataAccessException e) {
            throw new PersistenceException(operation, e);
        }
    }

    /**
     * Runs an operation that returns nothing.
     *
     * @param operation description of the operation in Spanish
     * @param action    database work to run
     * @throws PersistenceException if the database reports an error
     */
    public static void execute(String operation, Runnable action) {
        try {
            action.run();
        } catch (DataAccessException e) {
            throw new PersistenceException(operation, e);
        }
    }
}
