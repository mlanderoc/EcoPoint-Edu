package com.ecopoint.dto;

import com.ecopoint.model.Institution;

/**
 * Data to create or edit an institution (spec 004, R-320; spec 007, R-623).
 *
 * @param name      name, unique
 * @param address   address
 * @param mapCenter center of the map, or null
 * @param mapZoom   initial zoom of the map, or null for the default
 * @param settings  characterization settings, or null for the defaults
 * @author craulgomez
 * @version 1.0
 */
public record InstitutionRequest(String name, String address, GeoPointDto mapCenter, Integer mapZoom,
                                 SettingsDto settings) {

    /**
     * Builds the institution of the domain.
     *
     * @return the institution, without id
     */
    public Institution toModel() {
        Institution institution = new Institution(null, name, address);
        if (mapCenter != null) {
            institution.setMapCenter(mapCenter.toModel());
        }
        if (mapZoom != null) {
            institution.setMapZoom(mapZoom);
        }
        if (settings != null) {
            institution.setSettings(settings.toModel());
        }
        return institution;
    }
}
