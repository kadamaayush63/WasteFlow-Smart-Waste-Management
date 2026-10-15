package com.wasteflow.thingspeak;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThingSpeakFillLevelConverterTest {

    @Test
    void convertsConfiguredDistanceAnchorsToFillPercentages() {
        assertEquals(100, ThingSpeakFillLevelConverter.distanceToPercentage(2));
        assertEquals(60, ThingSpeakFillLevelConverter.distanceToPercentage(6));
        assertEquals(40, ThingSpeakFillLevelConverter.distanceToPercentage(8));
        assertEquals(0, ThingSpeakFillLevelConverter.distanceToPercentage(12));
    }

    @Test
    void clampsDistancesOutsideTheSensorRange() {
        assertEquals(100, ThingSpeakFillLevelConverter.distanceToPercentage(0));
        assertEquals(0, ThingSpeakFillLevelConverter.distanceToPercentage(12));
        assertEquals(0, ThingSpeakFillLevelConverter.distanceToPercentage(15.1));
        assertEquals(0, ThingSpeakFillLevelConverter.distanceToPercentage(20));
    }

    @Test
    void interpolatesBetweenConfiguredAnchors() {
        assertEquals(80, ThingSpeakFillLevelConverter.distanceToPercentage(4));
        assertEquals(30, ThingSpeakFillLevelConverter.distanceToPercentage(9));
    }

    @Test
    void mapsThingSpeakFieldsToTheirMatchingBinIds() {
        assertEquals(
                Map.of(1L, 100.0, 2L, 60.0, 3L, 0.0),
                ThingSpeakFillLevelConverter.sensorDistancesToBinFillPercentages(2, 6, 12)
        );
    }

    @Test
    void updatesAvailableFieldsWhenOtherSensorFieldsAreBlank() {
        assertEquals(
                Map.of(1L, 45.0545),
                ThingSpeakFillLevelConverter.sensorReadingsToBinFillPercentages("7.49455", "", null)
        );
    }

    @Test
    void rejectsFeedsWithNoSensorReadings() {
        assertThrows(IllegalArgumentException.class,
                () -> ThingSpeakFillLevelConverter.sensorReadingsToBinFillPercentages("", null, " "));
    }

    @Test
    void rejectsNonFiniteDistances() {
        assertThrows(IllegalArgumentException.class,
                () -> ThingSpeakFillLevelConverter.distanceToPercentage(Double.NaN));
    }
}
