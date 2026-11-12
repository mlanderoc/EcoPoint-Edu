package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable closed polygon that outlines a generation source on the
 * institution map (RN-004). The first vertex must equal the last one, and the
 * polygon must have at least 3 distinct vertices.
 *
 * @param vertices ordered vertices; the last one repeats the first
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record GeoPolygon(List<GeoPoint> vertices) {

    private static final int MIN_DISTINCT_VERTICES = 3;

    /**
     * Validates that the polygon is closed and has enough distinct vertices.
     *
     * @throws InvalidDataException if the polygon is missing, open or degenerate
     */
    public GeoPolygon {
        if (vertices == null || vertices.isEmpty()) {
            throw new InvalidDataException("geometria", "El polígono debe tener vértices.");
        }
        if (vertices.stream().anyMatch(Objects::isNull)) {
            throw new InvalidDataException("geometria", "El polígono no puede tener vértices vacíos.");
        }
        vertices = List.copyOf(vertices);
        if (!vertices.get(0).equals(vertices.get(vertices.size() - 1))) {
            throw new InvalidDataException("geometria",
                    "El polígono debe estar cerrado: el último vértice debe ser igual al primero.");
        }
        if (new HashSet<>(vertices).size() < MIN_DISTINCT_VERTICES) {
            throw new InvalidDataException("geometria",
                    "El polígono debe tener al menos " + MIN_DISTINCT_VERTICES + " vértices distintos.");
        }
    }

    /**
     * Returns the number of distinct vertices of the polygon.
     *
     * @return distinct vertex count (the closing vertex is not counted twice)
     */
    public int distinctVertexCount() {
        return new HashSet<>(vertices).size();
    }
}
