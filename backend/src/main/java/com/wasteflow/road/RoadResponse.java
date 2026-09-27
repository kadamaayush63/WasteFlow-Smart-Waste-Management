package com.wasteflow.road;

public record RoadResponse(
        Long id,
        Long sourceLocationId,
        Long destinationLocationId,
        double distance,
        double travelTimeMinutes,
        boolean bidirectional
) {
    public static RoadResponse from(Road road) {
        return new RoadResponse(
                road.getId(), road.getSource().getId(), road.getDestination().getId(),
                road.getDistance(), road.getTravelTimeMinutes(), road.isBidirectional()
        );
    }
}