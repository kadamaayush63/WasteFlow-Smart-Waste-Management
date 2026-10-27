package com.wasteflow.coa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    public record RouteWorkload(
            @DecimalMin(value = "0.0", message = "Distance cannot be negative") double distanceKm,
            @Min(value = 1, message = "Repetition count must be at least one")
            @Max(value = 100, message = "Repetition count cannot exceed 100") int repetitionCount
    ) {
    }

    public record RouteSummary(
            @NotEmpty @Size(max = 10, message = "A COA summary can contain at most 10 bins")
            List<@NotBlank @Size(max = 32, message = "Bin codes cannot exceed 32 characters") String> binCodes
    ) {
    }

    public record Leg(
            @DecimalMin(value = "0.0", message = "Distance cannot be negative") double distanceKm,
            @DecimalMin(value = "0.0", message = "Travel time cannot be negative") double travelTimeMinutes
    ) {
    }
}
