package com.wasteflow.road;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RoadRequest(
        @NotNull Long sourceLocationId,
        @NotNull Long destinationLocationId,
        @NotNull @DecimalMin("0.01") Double distance,
        @NotNull @DecimalMin("0.01") Double travelTimeMinutes,
        boolean bidirectional
) {
}