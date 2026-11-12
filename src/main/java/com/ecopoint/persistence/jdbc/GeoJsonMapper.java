package com.ecopoint.persistence.jdbc;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.GeoPoint;
import com.ecopoint.model.GeoPolygon;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the polygons of the generation sources to and from GeoJSON, the
 * format stored in the {@code geometria jsonb} column (spec 002, R-032).
 * GeoJSON writes each vertex as {@code [longitude, latitude]}.
 *
 * @author mlanderoc
 * @version 1.0
 */
public final class GeoJsonMapper {

    private static final ObjectMapper JSON = new ObjectMapper();

    private GeoJsonMapper() {
    }

    /**
     * Writes a polygon as a GeoJSON {@code Polygon}.
     *
     * @param polygon polygon to write, or null
     * @return GeoJSON text, or null if the polygon is null
     */
    public static String toGeoJson(GeoPolygon polygon) {
        if (polygon == null) {
            return null;
        }
        ObjectNode geometry = JSON.createObjectNode();
        geometry.put("type", "Polygon");
        ArrayNode ring = geometry.putArray("coordinates").addArray();
        for (GeoPoint vertex : polygon.vertices()) {
            ring.addArray().add(vertex.longitude()).add(vertex.latitude());
        }
        return geometry.toString();
    }

    /**
     * Reads a GeoJSON {@code Polygon} into a polygon of the domain.
     *
     * @param geoJson GeoJSON text, or null
     * @return the polygon, or null if the text is null
     * @throws PersistenceException if the stored text is not a valid GeoJSON polygon
     */
    public static GeoPolygon fromGeoJson(String geoJson) {
        if (geoJson == null) {
            return null;
        }
        try {
            JsonNode ring = JSON.readTree(geoJson).path("coordinates").path(0);
            List<GeoPoint> vertices = new ArrayList<>();
            for (JsonNode vertex : ring) {
                vertices.add(new GeoPoint(vertex.get(1).decimalValue(), vertex.get(0).decimalValue()));
            }
            return new GeoPolygon(vertices);
        } catch (JsonProcessingException | RuntimeException e) {
            throw new PersistenceException("leer la geometría GeoJSON de una fuente", e);
        }
    }
}
