package com.ecopoint.persistence;

import java.util.Map;

/**
 * Totals of the records typed and verified by each user (spec 003, R-133).
 *
 * @author mlanderoc
 * @version 1.0
 */
public interface UserActivityRepository {

    /**
     * Counts the records of one user.
     *
     * @param userId user id
     * @return the totals, all in zero if the user has no records
     */
    UserActivity findByUser(Long userId);

    /**
     * Counts the records of every user in a single query, for the users table.
     *
     * @return the totals by user id
     */
    Map<Long, UserActivity> findAll();
}
