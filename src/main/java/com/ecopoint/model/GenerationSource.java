package com.ecopoint.model;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    private static final int MAX_CODE_LENGTH = 6;
    private static final int PREFIX_LENGTH = 3;
    private static final String FALLBACK_PREFIX = "FU";
    private static final Set<String> CONNECTORS = Set.of("DE", "DEL", "LA", "LAS", "EL", "LOS", "Y", "E", "EN",
            "A", "AL", "PARA", "CON", "POR");

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
     * Builds a free code from the name, so the user does not have to invent it
     * (professor's review, point 4): the initials of the main words, or the
     * first three letters of a single word, followed by a number when taken.
     * "Aulas de Clase" gives {@code AC}; "Laboratorio" gives {@code LAB}.
     *
     * @param name       source name
     * @param takenCodes codes already used in the institution
     * @return a valid code that is not in {@code takenCodes}
     */
    public static String generateCode(String name, Set<String> takenCodes) {
        String prefix = codePrefix(name);
        if (!takenCodes.contains(prefix)) {
            return prefix;
        }
        for (int number = 2; ; number++) {
            String suffix = String.valueOf(number);
            String candidate = prefix.substring(0, Math.min(prefix.length(), MAX_CODE_LENGTH - suffix.length()))
                    + suffix;
            if (!takenCodes.contains(candidate)) {
                return candidate;
            }
        }
    }

    private static String codePrefix(String name) {
        String plain = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ");
        List<String> words = Arrays.stream(plain.trim().split("\\s+"))
                .filter(word -> !word.isEmpty() && !CONNECTORS.contains(word))
                .toList();
        String prefix;
        if (words.size() > 1) {
            StringBuilder initials = new StringBuilder();
            words.stream().limit(PREFIX_LENGTH).forEach(word -> initials.append(word.charAt(0)));
            prefix = initials.toString();
        } else if (words.size() == 1) {
            String word = words.get(0);
            prefix = word.substring(0, Math.min(PREFIX_LENGTH, word.length()));
        } else {
            prefix = "";
        }
        return prefix.length() < 2 ? FALLBACK_PREFIX : prefix;
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
