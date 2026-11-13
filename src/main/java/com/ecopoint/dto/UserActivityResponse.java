package com.ecopoint.dto;

import com.ecopoint.persistence.UserActivity;

/**
 * Totals of the records of a user (spec 003, R-133).
 *
 * @param populations         shift populations typed
 * @param bags                bags typed
 * @param quarterings         quarterings typed
 * @param densities           density measurements typed
 * @param typedRecords        all the records typed
 * @param verifications       records of other people verified or corrected
 * @param correctionsReceived corrections made to the records the user typed
 * @author craulgomez
 * @version 1.0
 */
public record UserActivityResponse(int populations, int bags, int quarterings, int densities, int typedRecords,
                                   int verifications, int correctionsReceived) {

    /**
     * Copies the totals read from the database.
     *
     * @param activity totals of the user, or null
     * @return the response, or null when there are no totals
     */
    public static UserActivityResponse from(UserActivity activity) {
        if (activity == null) {
            return null;
        }
        return new UserActivityResponse(activity.populations(), activity.bags(), activity.quarterings(),
                activity.densities(), activity.typedRecords(), activity.verifications(),
                activity.correctionsReceived());
    }
}
