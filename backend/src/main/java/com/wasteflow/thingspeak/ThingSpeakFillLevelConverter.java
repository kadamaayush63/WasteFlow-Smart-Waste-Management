package com.wasteflow.thingspeak;

import java.util.Map;
import java.util.HashMap;

public final class ThingSpeakFillLevelConverter {

    private ThingSpeakFillLevelConverter() {
    }

    public static double distanceToPercentage(double distanceCm) {
        if (!Double.isFinite(distanceCm)) {
            throw new IllegalArgumentException("Sensor distance must be a finite number");
        }
        if (distanceCm <= 2) {
            return 100;
        }
        if (distanceCm > 12) {
            return 0;
        }
        return (12 - distanceCm) * 10;
    }

    public static Map<Long, Double> sensorDistancesToBinFillPercentages(
            double sensor1DistanceCm,
            double sensor2DistanceCm,
            double sensor3DistanceCm
    ) {
        return Map.of(
                1L, distanceToPercentage(sensor1DistanceCm),
                2L, distanceToPercentage(sensor2DistanceCm),
                3L, distanceToPercentage(sensor3DistanceCm)
        );
    }

    public static Map<Long, Double> sensorReadingsToBinFillPercentages(
            String sensor1DistanceCm,
            String sensor2DistanceCm,
            String sensor3DistanceCm
    ) {
        Map<Long, Double> fillPercentages = new HashMap<>();
        addReading(fillPercentages, 1L, sensor1DistanceCm, "field1");
        addReading(fillPercentages, 2L, sensor2DistanceCm, "field2");
        addReading(fillPercentages, 3L, sensor3DistanceCm, "field3");
        if (fillPercentages.isEmpty()) {
            throw new IllegalArgumentException("ThingSpeak feed contains no sensor readings");
        }
        return Map.copyOf(fillPercentages);
    }

    private static void addReading(
            Map<Long, Double> fillPercentages,
            Long binId,
            String reading,
            String fieldName
    ) {
        if (reading == null || reading.isBlank()) {
            return;
        }
        try {
            fillPercentages.put(binId, distanceToPercentage(Double.parseDouble(reading)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ThingSpeak " + fieldName + " is not a valid distance", exception);
        }
    }
}
