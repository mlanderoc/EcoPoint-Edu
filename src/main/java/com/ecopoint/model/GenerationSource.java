package com.ecopoint.model;

import java.util.regex.Pattern;

/**
 * Place of an institution where waste is generated (classrooms, offices,
 * school store, PAE, green areas, restrooms, laboratory). It replaces the
 * former {@code Zone} and is drawn as a polygon on the institution map
 * (RN-001 to RN-005).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class GenerationSource {

    /** Valid source code: 2 to 6 uppercase letters or digits (RN-002). */
    public static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{2,6}$");

    private Long id;
    private Long institutionId;
    private String code;
    private String name;
    private SourceType type;
    private GeoPolygon geometry;
    private boolean active;

    /**
     * Creates an empty, active source.
     */
    public GenerationSource() {
        this.active = true;
    }

    /**
     * Creates an active source without geometry.
     *
     * @param id            identifier, or null if not saved yet
     * @param institutionId institution the source belongs to
     * @param code          short code such as {@code AU1} or {@code PAE}
     * @param name          descriptive name
     * @param type          source type
     */
    public GenerationSource(Long id, Long institutionId, String code, String name, SourceType type) {
        this();
        this.id = id;
        this.institutionId = institutionId;
        this.code = code;
        this.name = name;
        this.type = type;
    }

    /**
     * Indicates whether the code has the valid format (RN-002).
     *
     * @param code code to check
     * @return true for 2 to 6 uppercase letters or digits
     */
    public static boolean isValidCode(String code) {
        return code != null && CODE_PATTERN.matcher(code).matches();
    }

    /**
     * Indicates whether the source is drawn on the map.
     *
     * @return true if it has a polygon
     */
    public boolean hasGeometry() {
        return geometry != null;
    }

    /**
     * Indicates whether new field records may use this source (RN-005).
     *
     * @return true if the source is active
     */
    public boolean isAvailableForRegistration() {
        return active;
    }

    /**
     * Deactivates the source; it keeps its history but is not offered for new
     * records (RN-005).
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Activates the source again.
     */
    public void activate() {
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SourceType getType() {
        return type;
    }

    public void setType(SourceType type) {
        this.type = type;
    }

    public GeoPolygon getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoPolygon geometry) {
        this.geometry = geometry;
    }

    public boolean isActive() {
        return active;
    }
}
