package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link GeoPoint} and {@link GeoPolygon} (RN-004, CA-003).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class GeoPolygonTest {

    private static final GeoPoint A = GeoPoint.of("10.4630", "-73.2530");
    private static final GeoPoint B = GeoPoint.of("10.4630", "-73.2520");
    private static final GeoPoint C = GeoPoint.of("10.4640", "-73.2520");
    private static final GeoPoint D = GeoPoint.of("10.4640", "-73.2530");

    @Test
    void rn004_closedSquareIsAccepted() {
        GeoPolygon polygon = new GeoPolygon(List.of(A, B, C, D, A));

        assertEquals(4, polygon.distinctVertexCount());
    }

    @Test
    void ca003_threePointsNotClosedAreRejected() {
        assertThrows(InvalidDataException.class, () -> new GeoPolygon(List.of(A, B, C)));
    }

    @Test
    void ca003_latitudeOutOfRangeIsRejected() {
        assertThrows(InvalidDataException.class, () -> GeoPoint.of("95", "-73.25"));
    }

    @Test
    void rn004_longitudeOutOfRangeIsRejected() {
        assertThrows(InvalidDataException.class, () -> GeoPoint.of("10.46", "-181"));
    }

    @Test
    void rn004_closedPolygonWithOnlyTwoDistinctVerticesIsRejected() {
        assertThrows(InvalidDataException.class, () -> new GeoPolygon(List.of(A, B, A)));
    }

    @Test
    void rn004_emptyOrNullPolygonIsRejected() {
        assertThrows(InvalidDataException.class, () -> new GeoPolygon(List.of()));
        assertThrows(InvalidDataException.class, () -> new GeoPolygon(null));
    }

    @Test
    void rn004_pointsWithSamePositionAreEqualRegardlessOfScale() {
        GeoPoint shortScale = new GeoPoint(new BigDecimal("10.5"), new BigDecimal("-73.25"));
        GeoPoint longScale = new GeoPoint(new BigDecimal("10.5000000"), new BigDecimal("-73.2500"));

        assertEquals(shortScale, longScale);
    }

    @Test
    void rn004_polygonIsImmutableEvenIfSourceListChanges() {
        List<GeoPoint> vertices = new ArrayList<>(List.of(A, B, C, A));
        GeoPolygon polygon = new GeoPolygon(vertices);

        vertices.add(D);

        assertEquals(4, polygon.vertices().size());
    }
}
