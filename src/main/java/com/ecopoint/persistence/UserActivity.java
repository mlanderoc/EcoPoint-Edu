package com.ecopoint.persistence;

/**
 * How many records a user has typed and verified, and how many corrections his
 * records received (spec 003, R-133; spec 004, R-322 and R-351).
 *
 * @param userId              user id
 * @param populations         shift populations typed by the user
 * @param bags                bags typed by the user
 * @param quarterings         quarterings typed by the user
 * @param densities           density measurements typed by the user
 * @param verifications       records of other people verified or corrected by the user
 * @param correctionsReceived corrections made to records typed by the user
 * @author mlanderoc
 * @version 1.0
 */
public record UserActivity(Long userId, int populations, int bags, int quarterings, int densities,
                           int verifications, int correctionsReceived) {

    /**
     * Returns the activity of a user who has not typed or verified anything.
     *
     * @param userId user id
     * @return the activity with every total in zero
     */
    public static UserActivity none(Long userId) {
        return new UserActivity(userId, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Returns how many records the user typed.
     *
     * @return populations, bags, quarterings and densities together
     */
    public int typedRecords() {
        return populations + bags + quarterings + densities;
    }
}
