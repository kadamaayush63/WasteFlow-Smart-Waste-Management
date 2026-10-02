package com.wasteflow.coa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public final class CoaSimulationRequest {

    private CoaSimulationRequest() {
    }

    public record BinStatus(
            @DecimalMin(value = "0.0", message = "Current fill cannot be negative") double currentFillKg,
            @DecimalMin(value = "0.000001", message = "Capacity must be greater than zero") double capacityKg
    ) {
    }

    public record RouteTotals(
            @NotEmpty List<Leg> legs
    ) {
    }

    public record Leg(
            @DecimalMin(value = "0.0", message = "Distance cannot be negative") double distanceKm,
            @DecimalMin(value = "0.0", message = "Travel time cannot be negative") double travelTimeMinutes
    ) {
    }
}
