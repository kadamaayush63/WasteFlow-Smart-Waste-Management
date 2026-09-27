package com.wasteflow.location;

public record LocationResponse(Long id, String name, double xCoordinate, double yCoordinate) {
    public static LocationResponse from(Location location) {
        return new LocationResponse(location.getId(), location.getName(), location.getXCoordinate(), location.getYCoordinate());
    }
}