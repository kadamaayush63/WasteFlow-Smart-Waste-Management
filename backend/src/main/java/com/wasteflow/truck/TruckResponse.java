package com.wasteflow.truck;

public record TruckResponse(Long id, String truckCode, double capacityKg, double currentLoadKg, TruckStatus status, Long locationId) {
    public static TruckResponse from(Truck truck) {
        return new TruckResponse(truck.getId(), truck.getTruckCode(), truck.getCapacityKg(), truck.getCurrentLoadKg(), truck.getStatus(), truck.getLocation().getId());
    }
}