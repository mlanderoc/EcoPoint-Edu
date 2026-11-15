package com.ecopoint.model;

/**
 * Educational institution (school or university) whose solid waste is
 * characterized. It holds where its map opens (center and zoom) and the
 * configurable parameters of its characterization studies. An institution is
 * never deleted: it is deactivated (RN-006).
 *
 * @author EstefaniaMarquez
 * @version 3.0
 */
public class Institution {

    /** Zoom used when the institution has no zoom configured yet. */
    public static final int DEFAULT_MAP_ZOOM = 17;

    private Long id;
    private String name;
    private String address;
    private GeoPoint mapCenter;
    private int mapZoom;
    private CharacterizationSettings settings;
    private boolean active;

    /**
     * Creates an empty institution with the default map zoom and the default
     * characterization settings.
     */
    public Institution() {
        this.mapZoom = DEFAULT_MAP_ZOOM;
        this.active = true;
        this.settings = CharacterizationSettings.defaults();
    }

    /**
     * Creates an institution with its basic data, the default map zoom and the
     * default characterization settings.
     *
     * @param id      identifier, or null if not saved yet
     * @param name    institution name
     * @param address physical address
     */
    public Institution(Long id, String name, String address) {
        this();
        this.id = id;
        this.name = name;
        this.address = address;
    }

    /**
     * Indicates whether new studies may be created in this institution (RN-006).
     *
     * @return true if the institution is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Deactivates the institution; its studies, sources and records are kept,
     * but it does not admit new studies (RN-006).
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Activates the institution again.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Indicates whether the institution already has a map center configured.
     *
     * @return true if the map center is set
     */
    public boolean hasMapCenter() {
        return mapCenter != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public GeoPoint getMapCenter() {
        return mapCenter;
    }

    public void setMapCenter(GeoPoint mapCenter) {
        this.mapCenter = mapCenter;
    }

    public int getMapZoom() {
        return mapZoom;
    }

    public void setMapZoom(int mapZoom) {
        this.mapZoom = mapZoom;
    }

    public CharacterizationSettings getSettings() {
        return settings;
    }

    /**
     * Replaces the characterization settings; null restores the defaults.
     *
     * @param settings new settings, or null for the defaults
     */
    public void setSettings(CharacterizationSettings settings) {
        this.settings = settings != null ? settings : CharacterizationSettings.defaults();
    }
}
