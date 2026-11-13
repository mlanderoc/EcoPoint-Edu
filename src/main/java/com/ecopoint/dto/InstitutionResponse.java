package com.ecopoint.dto;

import com.ecopoint.model.Institution;

/**
 * Institution as the API returns it.
 *
 * @param id        institution id
 * @param name      name
 * @param address   address
 * @param mapCenter center of the map, or null
 * @param mapZoom   initial zoom of the map
 * @param settings  characterization settings
 * @param active    whether it admits new studies (RN-006)
 * @author craulgomez
 * @version 1.0
 */
public record InstitutionResponse(Long id, String name, String address, GeoPointDto mapCenter, int mapZoom,
                                  SettingsDto settings, boolean active) {

    /**
     * Copies an institution of the domain.
     *
     * @param institution domain institution
     * @return the response
     */
    public static InstitutionResponse from(Institution institution) {
        return new InstitutionResponse(institution.getId(), institution.getName(), institution.getAddress(),
                GeoPointDto.from(institution.getMapCenter()), institution.getMapZoom(),
                SettingsDto.from(institution.getSettings()), institution.isActive());
    }
}
